package com.example.demo.event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demo.engine.LifeDTO;
import com.example.demo.event.dto.EventResultDTO;

import jakarta.annotation.PreDestroy;

// C11 이벤트 AI 서술: AI 서술 대상 이벤트의 기본 문구를 Gemini가 그해 상황에 맞게 다시 씀
// 근거 자료(역사 사건 설명)는 이벤트 ID로 서버가 직접 조회해서 전달 (Tool Calling 없이 1번 호출)
@Service
public class NarrateService {

	private static final Logger log = LoggerFactory.getLogger(NarrateService.class);

	private static final int TIMEOUT_SEC = 4; // 평균 응답 2초 안팎 + 여유, 넘으면 기본 문구
	private static final int MAX_LEN = 200; // LOG_TEXT 1000바이트 (한글 약 330자) 안에 여유 있게
	private static final int POOL_SIZE = 8; // Gemini 동시 호출 일꾼 수 (발표 때 여러 명 동시 플레이 기준)

	// AI 역할과 규칙 (사용자는 못 봄)
	private static final String SYSTEM_PROMPT = """
			너는 인생 시뮬레이션 게임의 인생 기록을 쓰는 작가야.
			주어진 정보로 이 사람이 그해 겪은 일을 한국어 한두 문장(80자 안팎)으로 써.
			- 문장은 연도로 시작해. (예: 2010년, ...)
			- 기본 문구의 사건과 결과는 바꾸지 마.
			- 정보에 없는 일(실직, 부상, 이사, 병명, 장소 등)은 지어내지 마.
			- 상황(직장인, 학생, 직업 없음 등)은 문장에 쓰지 마. 지어내지 않게 하기 위한 참고용이야.
			- 가족 관계(혼자인지, 배우자, 자녀)는 가족 정보에 있는 대로만 써.
			- 게임 효과를 보고 이 사건이 이 사람에게 어떤 의미였는지 감정이나 장면으로 써. 효과 이름을 그대로 나열하지 마.
			- 역사 설명이 있으면 그중 구체적인 사실 하나를 시대 배경으로 써.
			- 나이가 0~5세면 아기·어린아이가 직접 겪은 일로 쓰지 마. 학교·일·시위·전투처럼 아이가 할 수 없는 일은 빼고, 집안 분위기나 부모님의 걱정·생활 변화로 써. (예: 1966년, 바깥이 어수선해지자 부모님은 아기 이름의 손을 꼭 잡고 집 밖에 잘 나가지 않았다.)
			- 나이가 6~12세면 아이가 겪을 수 있는 일(가족, 동네, 학교)로만 써.
			- 이름을 넣어 3인칭 과거형으로 써.
			- 따옴표, 제목, 설명 없이 문장만 출력해.
			""";

	private ChatClient gemini;
	private EventDAO eDAO;

	@Value("${narrate.enabled:true}") // 시뮬레이션처럼 많이 돌릴 때는 false로 끔
	private boolean enabled;

	// AI 서술 전용 일꾼 (자바 공용 풀과 섞이지 않게). 데몬 스레드라 서버 종료를 막지 않음
	private final ExecutorService pool = Executors.newFixedThreadPool(POOL_SIZE, task -> {
		Thread t = new Thread(task, "narrate");
		t.setDaemon(true);
		return t;
	});

	NarrateService(ChatClient.Builder builder, EventDAO eDAO) {
		this.gemini = builder.build();
		this.eDAO = eDAO;
	}

	// 서버가 꺼질 때 일꾼 정리
	@PreDestroy
	void close() {
		pool.shutdownNow();
	}

	// 이벤트 하나 서술 (부모님 별세처럼 한 건만 있을 때)
	public void narrate(LifeDTO life, EventResultDTO r, int age, int year) {
		narrateAll(life, List.of(r), age, year);
	}

	// 올해 이벤트 여러 개 서술: 대상만 골라 동시에 요청하고, 모두 합쳐 TIMEOUT_SEC 안에 온 것만 교체
	// 성공하면 문장 교체 + ai = true, 실패하면 기본 문구 그대로
	public void narrateAll(LifeDTO life, List<EventResultDTO> results, int age, int year) {
		if (!enabled || results == null || results.isEmpty())
			return;

		// 1. 대상 고르기 + 요청 보내기 (보내기만 하고 기다리지 않음)
		List<EventResultDTO> targets = new ArrayList<>();
		List<CompletableFuture<String>> futures = new ArrayList<>();
		for (EventResultDTO r : results) {
			if (!r.isAiNarrate() || r.isChoice())
				continue;
			targets.add(r);
			futures.add(request(life, r, age, year));
		}

		// 2. 결과 받기: 마감 시각을 하나로 정해서 전체가 TIMEOUT_SEC 를 넘지 않게
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SEC);
		for (int i = 0; i < targets.size(); i++) {
			EventResultDTO r = targets.get(i);
			CompletableFuture<String> f = futures.get(i);
			try {
				long left = Math.max(0, deadline - System.nanoTime()); // 마감까지 남은 시간
				String text = clean(f.get(left, TimeUnit.NANOSECONDS));
				if (text != null) {
					r.setLogText(text);
					r.setAi(true);
				}
			} catch (Exception ex) {
				// 시간 초과·호출 실패 → 기본 문구 사용 (C11 요구사항 3), 원인은 경고 로그로 한 줄만 남김
				f.cancel(true); // 아직 안 끝났으면 더 기다리지 않음
				Throwable root = ex;
				while (root.getCause() != null) {
					root = root.getCause(); // 감싼 예외를 벗겨서 진짜 원인 찾기
				}
				log.warn("C11 AI 서술 실패, 기본 문구 사용 ({}): {}", r.getEventId(), root.toString());
			}
		}
	}

	// Gemini 요청 하나: 근거 정보(DB 조회)는 지금 스레드에서 만들고, 호출만 전용 일꾼에게 맡김
	private CompletableFuture<String> request(LifeDTO life, EventResultDTO r, int age, int year) {
		try {
			String userPrompt = buildInfo(life, r, age, year);
			return CompletableFuture.supplyAsync(() -> {
				ChatResponse resp = gemini.prompt().system(SYSTEM_PROMPT).user(userPrompt).call().chatResponse();
				return resp.getResult().getOutput().getText();
			}, pool);
		} catch (Exception ex) {
			return CompletableFuture.failedFuture(ex); // 근거 조회가 실패해도 위에서 똑같이 기본 문구 처리
		}
	}

	// 근거 정보: 나라·연도·나이·상황·가족·사건·효과 + 역사 이벤트면 C16 사건 설명
	private String buildInfo(LifeDTO life, EventResultDTO r, int age, int year) {
		String country = eDAO.selectCountryName(life.getCurrentCountry());
		String desc = "HIST".equals(r.getEventType()) ? eDAO.selectHistDescription(r.getEventId()) : null;
		if (desc != null && desc.length() > 600)
			desc = desc.substring(0, 600); // 설명이 길면 앞부분만

		// 지금 상황 (AI가 개인 사정을 지어내지 않게, 나이에 맞는 말로)
		String status;
		if ("EMPLOYED".equals(life.getJobStatus()))
			status = "직장인";
		else if ("ENROLLED".equals(life.getSchoolStatus()))
			status = "학생";
		else if (age < 7)
			status = "어린아이";
		else if (age < 18)
			status = "청소년";
		else if (age >= 60)
			status = "노년";
		else
			status = "직업 없음";

		// 가족 정보 (AI가 '홀로' 같은 가족 상황을 지어내지 않게)
		String family = ("Y".equals(life.getMarriedYn()) ? "배우자 있음" : "배우자 없음") + ", 자녀 " + life.getChildCount() + "명";

		// 이 사건의 큰 효과만 (작은 값은 AI가 부풀리지 않게 빼고 전달)
		List<String> big = new ArrayList<>();
		if (Math.abs(r.getEffHealth()) >= 5)
			big.add(r.getEffHealth() > 0 ? "건강 좋아짐" : "건강 나빠짐");
		if (Math.abs(r.getEffHappiness()) >= 5)
			big.add(r.getEffHappiness() > 0 ? "행복 커짐" : "행복 줄어듦");
		if (Math.abs(r.getEffKnowledge()) >= 5)
			big.add(r.getEffKnowledge() > 0 ? "지식 늘어남" : "지식 줄어듦");
		if (Math.abs(r.getEffRelation()) >= 5)
			big.add(r.getEffRelation() > 0 ? "인간관계 가까워짐" : "인간관계 멀어짐");
		if (Math.abs(r.getEffWealthRate()) >= 0.10)
			big.add(r.getEffWealthRate() > 0 ? "재산 늘어남" : "재산 줄어듦");
		String effects = big.isEmpty() ? "큰 변화 없음" : String.join(", ", big);

		return """
				이름: %s
				성별: %s
				나이: %d세
				연도: %d년
				사는 나라: %s
				상황: %s
				가족: %s
				사건: %s
				기본 문구: %s
				게임 효과: %s
				역사 설명: %s
				""".formatted(life.getName(), "M".equals(life.getGender()) ? "남성" : "여성", age, year,
				country == null ? life.getCurrentCountry() : country, status, family, r.getTitle(), r.getLogText(),
				effects, desc == null ? "없음" : desc);
	}

	// 응답 정리: 줄바꿈·따옴표 제거, 비었거나 너무 길면 실패로 보고 기본 문구 사용
	private String clean(String text) {
		if (text == null)
			return null;
		text = text.replace("\n", " ").replace("\"", "").trim();
		if (text.isEmpty() || text.length() > MAX_LEN)
			return null;
		return text;
	}
}