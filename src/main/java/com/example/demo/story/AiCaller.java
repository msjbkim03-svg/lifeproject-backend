package com.example.demo.story;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// C19·C20 공통: Gemini 부르기 (11/25 다시 만듦)
//  - 기본 모델부터 부르고, 정해진 시간(hedgeMs) 안에 답이 없으면 예비 모델도 같이 불러서 먼저 온 답을 씀
//    (하루 무료 한도가 찬 모델은 구글 쪽에서 몇 번 다시 시도하느라 10초 넘게 답이 없음 → 기다리지 않고 바로 다음 모델로)
//  - 실패하면 원인을 종류(kind)로 나눠서 AiFail 로 던짐 → 화면에 '한도 초과', '키 문제' 처럼 알맞은 안내
//  - 한도가 찬 모델은 기억해 두고 건너뜀: 하루 한도 30분, 분당 한도 · 느림 1~2분
//  - 콘솔에는 한 줄로 원인만 (API 키는 가려서)
@Component
public class AiCaller {

	private static final Logger log = LoggerFactory.getLogger(AiCaller.class);

	// AI 실패: kind = KEY 키 · QUOTA 한도 · MODEL 모델 이름 · BLOCKED 안전 차단 · BUSY 서버 붐빔 · TIMEOUT 시간 초과 · EMPTY 빈 답 · OTHER 그 밖
	public static class AiFail extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public final String kind;		// 종류
		public final String userMessage;	// 화면에 보여 줄 안내
		public final String detail;		// 원인 (콘솔 · 점검용, 키는 가림)

		public AiFail(String kind, String userMessage, String detail) {
			super(kind + ": " + detail);
			this.kind = kind;
			this.userMessage = userMessage;
			this.detail = detail;
		}
	}

	private final ChatClient chatClient;

	// AI 부르는 일만 따로 돌리는 스레드: 기본 공용 스레드(ForkJoin)를 쓰면 늦게 끝나는 호출이 다음 요청들을 막음
	private static final ExecutorService POOL = Executors.newFixedThreadPool(16, r -> {
		Thread t = new Thread(r, "ai-call");
		t.setDaemon(true);
		return t;
	});

	@Value("${spring.ai.google.genai.chat.model:(기본)}")
	private String model;

	// 예비 모델 (쉼표로 여러 개, 앞에서부터). 모델마다 무료 한도가 따로라 기본 모델 한도가 차도 이어서 씀
	@Value("${ai.backup-models:gemini-3.1-flash-lite,gemini-3.5-flash}")
	private String backupModels;

	private static final long DAY_SKIP_MS = 30 * 60 * 1000L;	// 하루 한도 찬 모델 건너뛰는 시간
	private static final long MINUTE_SKIP_MS = 60 * 1000L;		// 분당 한도 찬 모델
	private static final long SLOW_SKIP_MS = 2 * 60 * 1000L;	// 붐비거나 끝내 답이 없던 모델
	private final Map<String, Long> dayFull = new ConcurrentHashMap<>();	// 하루 한도 찬 모델 → 이 시각까지 건너뜀
	private final Map<String, Long> fullUntil = new ConcurrentHashMap<>();	// 잠깐 막힌 모델 → 이 시각까지 건너뜀

	// ChatClient.Builder 는 spring-ai 가 application.properties 의 Gemini 설정으로 만들어 줌
	public AiCaller(ChatClient.Builder chatClientBuilder) {
		this.chatClient = chatClientBuilder.build();
	}

	public String model() {
		return model;
	}

	// 짧은 답(묻기 · 점검): 4초 안에 답이 없으면 다음 모델도 같이 부름
	public String call(String system, String user, int timeoutSec) {
		return call(system, user, timeoutSec, 4000);
	}

	// system: 규칙, user: 자료 + 요청, timeoutSec: 모두 합쳐 기다릴 시간, hedgeMs: 이 시간 안에 답이 없으면 다음 모델도 부름
	//   → 먼저 온 답 (모두 실패하면 마지막 원인으로 AiFail)
	public String call(String system, String user, int timeoutSec, long hedgeMs) {
		long t0 = System.currentTimeMillis();
		long end = t0 + timeoutSec * 1000L;
		List<String> order = usable();
		if (order.isEmpty()) {
			throw new AiFail("QUOTA", "AI 사용 한도를 넘었어요. 잠시 뒤 다시 해 주세요.", "모든 모델이 한도 초과 상태 (잠시 건너뜀)");
		}
		List<CompletableFuture<String>> jobs = new ArrayList<>();
		List<String> names = new ArrayList<>();
		AiFail[] last = new AiFail[1];
		int next = 0;
		long nextAt = 0; // 다음 모델을 부를 시각
		while (System.currentTimeMillis() < end) {
			long now = System.currentTimeMillis();
			// 부를 차례: 처음 · 앞 모델들이 다 실패했거나 hedgeMs 가 지났을 때
			boolean allFailed = !jobs.isEmpty() && jobs.stream().allMatch(CompletableFuture::isCompletedExceptionally);
			if (next < order.size() && (jobs.isEmpty() || allFailed || now >= nextAt)) {
				String m = order.get(next++);
				names.add(m);
				jobs.add(start(m, system, user, last));
				nextAt = now + hedgeMs;
				continue;
			}
			for (int i = 0; i < jobs.size(); i++) {
				CompletableFuture<String> j = jobs.get(i);
				if (j.isDone() && !j.isCompletedExceptionally()) {
					String text = j.join();
					for (int k = 0; k < i; k++) {
						if (!jobs.get(k).isDone()) {
							fullUntil.put(names.get(k), System.currentTimeMillis() + SLOW_SKIP_MS); // 앞 모델은 답이 없었음 → 잠깐 뒤로
						}
					}
					log.info("[AI] 성공 {} {}ms, {}자", names.get(i), System.currentTimeMillis() - t0, text.length());
					return text;
				}
			}
			if (allFailed && next >= order.size()) {
				break; // 부를 모델을 다 불렀고 모두 실패
			}
			try {
				Thread.sleep(80);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		if (last[0] != null && (jobs.stream().allMatch(CompletableFuture::isDone))) {
			throw last[0];
		}
		log.warn("[AI] 실패 TIMEOUT ({}ms, 부른 모델 {})", System.currentTimeMillis() - t0, names);
		throw new AiFail("TIMEOUT", "AI 답이 너무 늦어요. 잠시 뒤 다시 해 주세요.", timeoutSec + "초 안에 답이 안 옴");
	}

	// 지금 부를 수 있는 모델 순서: 기본 → 예비, 하루 한도 찬 모델은 빼고, 잠깐 막힌 모델은 맨 뒤로
	private List<String> usable() {
		long now = System.currentTimeMillis();
		List<String> ok = new ArrayList<>();
		List<String> later = new ArrayList<>();
		for (String m : models()) {
			Long hard = dayFull.get(m);
			Long soft = fullUntil.get(m);
			if (hard != null && hard > now) {
				continue;
			}
			if (soft != null && soft > now) {
				later.add(m);
			} else {
				ok.add(m);
			}
		}
		ok.addAll(later);
		return ok;
	}

	// 모델 하나 부르기 (따로 도는 일): 답 글자, 실패하면 원인을 기록하고 AiFail 로 끝남
	private CompletableFuture<String> start(String m, String system, String user, AiFail[] last) {
		long t0 = System.currentTimeMillis();
		return CompletableFuture.supplyAsync(() -> {
			try {
				// Spring AI 2.0: 요청마다 바꿀 옵션은 build() 하지 않은 빌더로 넘김 (기본 설정 위에 모델 이름만 덮어씀)
				ChatResponse resp = chatClient.prompt().options(GoogleGenAiChatOptions.builder().model(m))
						.system(system).user(user).call().chatResponse();
				String text = AiText.join(resp);
				if (text == null || text.isBlank()) {
					throw new AiFail("EMPTY", "AI가 빈 답을 돌려줬어요. 다시 해 주세요.", "빈 답");
				}
				return text.trim();
			} catch (RuntimeException e) {
				AiFail f = e instanceof AiFail ? (AiFail) e : classify(e);
				if ("QUOTA".equals(f.kind)) {
					mark(m, f.detail);
				} else if ("TIMEOUT".equals(f.kind) || "BUSY".equals(f.kind)) {
					fullUntil.put(m, System.currentTimeMillis() + SLOW_SKIP_MS);
				}
				log.warn("[AI] 실패 {} {} ({}ms): {}", m, f.kind, System.currentTimeMillis() - t0, f.detail);
				last[0] = f;
				throw f;
			}
		}, POOL);
	}

	// 한도 초과 기록: 하루 한도면 30분, 분당 한도(짧은 시간에 많이 부름)면 1분 건너뜀
	private void mark(String m, String detail) {
		String d = detail == null ? "" : detail;
		if (d.contains("PerDay") || d.contains("per day")) {
			dayFull.put(m, System.currentTimeMillis() + DAY_SKIP_MS);
		} else {
			fullUntil.put(m, System.currentTimeMillis() + MINUTE_SKIP_MS);
		}
	}

	// 쓸 모델 목록: 기본 모델 + 예비 모델 (겹치면 한 번만)
	private List<String> models() {
		List<String> out = new ArrayList<>();
		out.add(model);
		for (String m : backupModels.split(",")) {
			String t = m.trim();
			if (!t.isEmpty() && !out.contains(t)) {
				out.add(t);
			}
		}
		return out;
	}

	// 점검용: 모델별 상태 (한도 차서 건너뛰는 중인지)
	public Map<String, Object> status() {
		Map<String, Object> out = new LinkedHashMap<>();
		long now = System.currentTimeMillis();
		for (String m : models()) {
			Long hard = dayFull.get(m);
			Long soft = fullUntil.get(m);
			out.put(m, hard != null && hard > now ? "하루 한도 초과 (30분 건너뜀)" : soft != null && soft > now ? "잠깐 막힘 (분당 한도 · 느림)" : "사용 가능");
		}
		return out;
	}

	// 예외 → 종류 (감싼 예외를 모두 벗겨서 메시지를 모아 봄)
	private AiFail classify(Throwable e) {
		StringBuilder all = new StringBuilder();
		Throwable root = e;
		for (Throwable c = e; c != null; c = c.getCause()) {
			all.append(c.toString()).append(" | ");
			root = c;
		}
		String m = all.toString();
		String detail = hide(root.toString());
		if (detail.length() > 400) {
			detail = detail.substring(0, 400) + "…";
		}
		// 한도 먼저 (한도 안내 글에 다른 숫자가 섞여 있어도 헷갈리지 않게)
		if (code(m, "429") || has(m, "RESOURCE_EXHAUSTED", "exceeded your current quota", "Quota exceeded", "rate limit")) {
			String kindOf = m.contains("PerDay") || m.contains("per day") ? " [PerDay]" : m.contains("PerMinute") || m.contains("per minute") ? " [PerMinute]" : "";
			return new AiFail("QUOTA", "AI 사용 한도를 넘었어요. 잠시 뒤 다시 해 주세요.", firstLine(detail) + kindOf);
		}
		if (code(m, "401") || code(m, "403") || has(m, "API_KEY_INVALID", "API key not valid", "UNAUTHENTICATED", "PERMISSION_DENIED")) {
			return new AiFail("KEY", "AI 키 설정에 문제가 있어요. (관리자 확인 필요)", detail);
		}
		if (code(m, "404") || has(m, "NOT_FOUND", "is not found", "not supported for generateContent")) {
			return new AiFail("MODEL", "AI 모델 설정이 맞지 않아요. (관리자 확인 필요)", detail);
		}
		if (has(m, "SAFETY", "PROHIBITED_CONTENT", "blockReason")) {
			return new AiFail("BLOCKED", "AI가 이 내용에는 답하지 않았어요. 다른 질문을 해 주세요.", detail);
		}
		if (code(m, "503") || code(m, "500") || has(m, "UNAVAILABLE", "overloaded")) {
			return new AiFail("BUSY", "AI 서버가 붐벼요. 잠시 뒤 다시 해 주세요.", detail);
		}
		if (has(m, "Timeout", "timed out", "SocketTimeout")) {
			return new AiFail("TIMEOUT", "AI 답이 너무 늦어요. 잠시 뒤 다시 해 주세요.", detail);
		}
		return new AiFail("OTHER", "AI가 답하지 못했어요. 잠시 뒤 다시 해 주세요.", detail);
	}

	// HTTP 오류 번호 (앞뒤가 숫자 · 점이 아닌 곳만: '7.98540...' 같은 숫자 속 403 은 빼고)
	private static boolean code(String s, String c) {
		return java.util.regex.Pattern.compile("(?<![0-9.])" + c + "(?![0-9.])").matcher(s).find();
	}

	private static String firstLine(String s) {
		int i = s.indexOf("Quota exceeded");
		String t = i >= 0 ? s.substring(i) : s;
		int n = t.indexOf('\n');
		return n > 0 ? t.substring(0, n) : t;
	}

	private static boolean has(String s, String... words) {
		for (String w : words) {
			if (s.contains(w)) {
				return true;
			}
		}
		return false;
	}

	// 메시지 속 API 키 가리기
	private static String hide(String s) {
		return s.replaceAll("(?i)(key=)[^&\\s\"]+", "$1***").replaceAll("AIza[0-9A-Za-z_\\-]{20,}", "AIza***");
	}
}
