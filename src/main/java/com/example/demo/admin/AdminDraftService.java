package com.example.demo.admin;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import com.example.demo.event.EventDAO;
import com.example.demo.event.dto.EventChoiceDTO;

// C10-6 역사 이벤트 게임 문구: Gemini 초안 일괄 생성 → 관리자 검수 → 저장(IS_REVIEWED = Y)
// 초안은 DB에 저장하지 않고 화면으로만 보냄
@Service
public class AdminDraftService {

	private static final Logger log = LoggerFactory.getLogger(AdminDraftService.class);

	private static final int MAX_IDS = 10; // 한 번에 묶는 이벤트 수 (Gemini 1번 호출)
	private static final int TIMEOUT_SEC = 30; // 10건 묶음이라 C11(4초)보다 넉넉하게
	private static final int DESC_LEN = 600; // 사건 설명은 앞부분만

	// 게임(EventService)이 실제로 바꿔 주는 자리 표시만 허용
	private static final List<String> ALLOWED = List.of("{연도}", "{이름}", "{나이}", "{나라}");
	private static final Pattern PLACEHOLDER = Pattern.compile("\\{[^}]*\\}");

	private static final String SYSTEM_PROMPT = """
			너는 아시아 역사 기반 인생 시뮬레이션 게임의 문구 작가야.
			역사 사건마다 인생 기록에 들어갈 기본 문장을 한국어 한 문장(40~60자)으로 짧게 써. 60자를 넘기지 마.
			- 문장은 반드시 {연도}년, 으로 시작해. {연도}는 게임이 실제 연도로 바꿔.
			- 쓸 수 있는 자리 표시는 {연도}, {이름}, {나라}뿐이야. 다른 중괄호는 쓰지 마.
			- {이름} 뒤 조사는 은(는), 이(가), 을(를)처럼 써. 게임이 받침에 맞게 바꿔.
			- 이 문장은 사건 기간 중 어느 해에나 나올 수 있어. 기간이 여러 해면 그해에도 맞게 '이어졌다', '계속됐다' 같은 표현을 써.
			- 사건 설명에 있는 사실만 써. 숫자, 인물, 장소를 지어내지 마.
			- 사건 설명의 핵심 사실 하나만 써. 분위기 묘사는 짧은 표현 하나면 충분해.
			- 개인이 겪은 결과(다침, 실직, 이사 등)는 쓰지 마. 효과는 게임이 따로 계산해.
			- 선택이 Y인 사건은 선택지를 고민하게 되는 상황으로 문장을 끝내. 어떤 선택을 할지, 결과는 쓰지 마.
			- 3인칭 과거형으로, 따옴표 없이 써.
			- 받은 사건마다 eventId를 그대로 돌려줘.
			""";

	// Gemini 응답 한 건 (Spring AI 가 JSON → 이 형태로 바꿔 줌)
	public record Draft(String eventId, String text) {
	}

	private ChatClient gemini;
	private AdminEventDAO aDAO;
	private EventDAO eDAO;

	public AdminDraftService(ChatClient.Builder builder, AdminEventDAO aDAO, EventDAO eDAO) {
		this.gemini = builder.build();
		this.aDAO = aDAO;
		this.eDAO = eDAO;
	}

	// 초안 만들기: 이벤트마다 { 근거 정보 + 지금 문장 + draft(초안) + error(실패 이유) }
	public List<Map<String, Object>> drafts(List<String> eventIds) {
		if (eventIds == null || eventIds.isEmpty() || eventIds.size() > MAX_IDS) {
			throw new IllegalArgumentException("이벤트 ID를 1~" + MAX_IDS + "개 보내 주세요.");
		}

		// 1. 근거 모으기 (요청 순서 유지 → LinkedHashMap)
		Map<String, Map<String, Object>> byId = new LinkedHashMap<>();
		StringBuilder sb = new StringBuilder();
		for (String id : eventIds) {
			AdminEventRowDTO row = aDAO.selectEventRow(id);
			if (row == null || !"HIST".equals(row.getEventType())) {
				throw new IllegalArgumentException("역사 이벤트가 아니에요: " + id);
			}
			String desc = eDAO.selectHistDescription(id);
			if (desc != null && desc.length() > DESC_LEN) {
				desc = desc.substring(0, DESC_LEN);
			}
			String country = eDAO.selectCountryName(row.getCountryCode());

			// 선택 이벤트면 선택지 문구도 근거로 (문장이 선택으로 자연스럽게 이어지게)
			String choiceText = "없음";
			if ("Y".equals(row.getIsChoice())) {
				StringBuilder cs = new StringBuilder();
				for (EventChoiceDTO c : eDAO.selectChoices(id)) {
					cs.append(c.getChoiceOrder()).append(") ").append(c.getChoiceText()).append("  ");
				}
				choiceText = cs.toString().trim();
			}

			Map<String, Object> item = new LinkedHashMap<>();
			item.put("eventId", id);
			item.put("title", row.getTitle());
			item.put("histTitle", row.getHistTitle());
			item.put("country", country);
			item.put("startYear", row.getStartYear());
			item.put("endYear", row.getEndYear());
			item.put("isChoice", row.getIsChoice());
			item.put("description", desc);
			item.put("currentText", row.getBaseText());
			item.put("draft", null);
			item.put("error", null);
			byId.put(id, item);

			sb.append("""
					eventId: %s
					나라: %s
					기간: %s~%s년
					사건: %s
					게임 제목: %s
					선택: %s
					선택지: %s
					사건 설명: %s

					""".formatted(id, country, row.getStartYear(), row.getEndYear(), row.getHistTitle(), row.getTitle(),
					row.getIsChoice(), choiceText, desc == null ? "없음" : desc));
		}
		String userPrompt = sb.toString();

		// 2. Gemini 1번 호출 (다른 스레드에서 실행, TIMEOUT_SEC 까지만 기다림)
		// 형식 안내(JSON으로 답하기)를 붙여 보내고, 받은 글을 직접 List<Draft>로 바꿈
		BeanOutputConverter<List<Draft>> converter = new BeanOutputConverter<>(
				new ParameterizedTypeReference<List<Draft>>() {
				});
		List<Draft> result;
		try {
			ChatResponse res = CompletableFuture.supplyAsync(() -> gemini.prompt().system(SYSTEM_PROMPT)
					.user(userPrompt + "\n" + converter.getFormat()).call().chatResponse())
					.get(TIMEOUT_SEC, TimeUnit.SECONDS);
			String raw = res.getResult().getOutput().getText();
			result = converter.convert(raw);
		} catch (Exception ex) {
			Throwable root = ex;
			while (root.getCause() != null) {
				root = root.getCause();
			}
			log.warn("C10 역사 문장 초안 실패: {}", root.toString());
			for (Map<String, Object> item : byId.values()) {
				item.put("error", "AI 호출 실패");
			}
			return new ArrayList<>(byId.values());
		}

		// 3. 결과를 이벤트 ID로 맞춰 넣기 (규칙에 안 맞으면 error 에 이유)
		if (result != null) {
			for (Draft d : result) {
				if (d == null || d.eventId() == null || !byId.containsKey(d.eventId())) {
					continue; // 요청하지 않은 ID 는 무시
				}
				String text = clean(d.text());
				String err = checkText(text, true);
				if (err == null) {
					byId.get(d.eventId()).put("draft", text);
				} else {
					byId.get(d.eventId()).put("error", err);
				}
			}
		}
		for (Map<String, Object> item : byId.values()) {
			if (item.get("draft") == null && item.get("error") == null) {
				item.put("error", "AI가 이 사건의 문장을 돌려주지 않았어요.");
			}
		}
		return new ArrayList<>(byId.values());
	}

	// 검수 저장: 문장 검사 → BASE_TEXT 저장 + IS_REVIEWED = 'Y'
	// 없는 이벤트·역사 이벤트가 아니면 false → 404
	public boolean saveBaseText(String eventId, String baseText) {
		String text = baseText == null ? null : baseText.trim();
		String err = checkText(text, false); // 관리자가 직접 고친 문장은 시작 형식까지는 강제하지 않음
		if (err != null) {
			throw new IllegalArgumentException(err);
		}
		return aDAO.updateBaseText(eventId, text) == 1;
	}

	// 줄바꿈·따옴표 제거
	private String clean(String text) {
		if (text == null) {
			return null;
		}
		return text.replace("\n", " ").replace("\"", "").trim();
	}

	// 문장 검사: 문제없으면 null, 있으면 이유
	private String checkText(String text, boolean needYearStart) {
		if (text == null || text.isEmpty()) {
			return "문장이 비어 있어요.";
		}
		if (needYearStart && !text.startsWith("{연도}년")) {
			return "{연도}년으로 시작하지 않아요.";
		}
		if (text.getBytes(StandardCharsets.UTF_8).length > 1000) {
			return "문장이 너무 길어요. (한글 약 330자까지)";
		}
		Matcher m = PLACEHOLDER.matcher(text);
		while (m.find()) {
			if (!ALLOWED.contains(m.group())) {
				return "쓸 수 없는 자리 표시예요: " + m.group();
			}
		}
		return null;
	}
}