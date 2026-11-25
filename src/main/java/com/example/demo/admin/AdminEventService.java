package com.example.demo.admin;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.event.EventDAO;
import com.example.demo.event.EventService;
import com.example.demo.event.dto.EventChoiceDTO;
import com.example.demo.event.dto.EventTemplateDTO;
import com.example.demo.event.dto.GameEventDTO;

//A03 이벤트 관리
@Service
public class AdminEventService {

	private final AdminEventDAO aDAO;
	private final EventDAO eDAO; // 선택지·역사 설명은 게임에서 쓰는 쿼리 그대로 사용
	private final EventService eServ; // 템플릿 저장 후 캐시 비우기

	public AdminEventService(AdminEventDAO aDAO, EventDAO eDAO, EventService eServ) {
		this.aDAO = aDAO;
		this.eDAO = eDAO;
		this.eServ = eServ;
	}

	// 목록: 빈 문자열·공백은 조건 없음으로
	public List<AdminEventRowDTO> list(String type, String reviewed, String aiNarrate, String keyword) {
		return aDAO.selectEventList(blankToNull(type), blankToNull(reviewed), blankToNull(aiNarrate),
				blankToNull(keyword));
	}

	// 상세: 이벤트 + 머리 정보 + (인생) 템플릿 / (역사) 사건 설명 + 선택지
	// 없는 이벤트면 null → Controller 에서 404
	public Map<String, Object> detail(String eventId) {
		GameEventDTO e = aDAO.selectEvent(eventId);
		if (e == null) {
			return null;
		}

		Map<String, Object> result = new LinkedHashMap<>(); // 화면에 보일 순서대로
		result.put("event", e);
		result.put("info", aDAO.selectEventRow(eventId));

		if ("LIFE".equals(e.getEventType())) {
			result.put("template", aDAO.selectTemplate(e.getTemplateId()));
		} else {
			result.put("description", eDAO.selectHistDescription(eventId));
		}

		result.put("choices", eDAO.selectChoices(eventId));
		return result;
	}

	// ================= 등록·수정 =================

	// 등록: ID 형식·유형 확인 → 빈 값 정리 → 기본값 → 검사 → INSERT
	// 같은 ID 가 있으면 DuplicateKeyException, 없는 템플릿·사건 ID 면
	// DataIntegrityViolationException (Controller 에서 처리)
	public void createEvent(GameEventDTO e) {
		e.setEventId(blankToNull(e.getEventId()));
		if (e.getEventId() == null || !e.getEventId().matches("[A-Z0-9_]{1,50}")) {
			throw new IllegalArgumentException("이벤트 ID는 영문 대문자·숫자·_ 로 50자까지예요.");
		}
		if (!"LIFE".equals(e.getEventType())) { // 역사 이벤트는 사건 자료(HISTORY_EVENT)와 함께 DB에서 등록
			throw new IllegalArgumentException("관리자 화면에서는 인생 이벤트만 등록할 수 있어요.");
		}
		normalize(e);

		// 등록할 때만 기본값 (수정은 받은 값 그대로)
		if (e.getEffectScale() == null)
			e.setEffectScale(1.0);
		if (e.getIsOnce() == null)
			e.setIsOnce("N");
		if (e.getIsChoice() == null)
			e.setIsChoice("N");
		if (e.getAiNarrate() == null)
			e.setAiNarrate("N");
		if (e.getIsReviewed() == null)
			e.setIsReviewed("Y"); // 관리자가 직접 쓴 이벤트

		validate(e);
		aDAO.insertEvent(e);
	}

	// 수정: 없는 이벤트면 false → 404
	// ID·유형은 바꾸지 않음 (주소의 ID, DB 의 유형 사용)
	public boolean updateEvent(String eventId, GameEventDTO e) {
		GameEventDTO old = aDAO.selectEvent(eventId);
		if (old == null) {
			return false;
		}
		e.setEventId(eventId);
		e.setEventType(old.getEventType());
		normalize(e);
		validate(e);
		return aDAO.updateEvent(e) == 1;
	}

	// AI 서술 지정: Y/N 만, 없는 이벤트면 false
	public boolean setAiNarrate(String eventId, String value) {
		checkYN(value, "AI 서술");
		return aDAO.updateAiNarrate(eventId, value) == 1;
	}

	// ================= 선택지 =================

	// 결과 동작 목록 (화면 드롭다운용)
	public List<String> resultActions() {
		return aDAO.selectResultActions();
	}

	// 선택지 추가: 없는 이벤트면 null → 404
	// 순서를 안 보내면 마지막 다음 번호, 보내면 겹치는지 확인
	public EventChoiceDTO createChoice(String eventId, EventChoiceDTO c) {
		GameEventDTO ev = aDAO.selectEvent(eventId);
		if (ev == null) {
			return null;
		}
		if (!"Y".equals(ev.getIsChoice())) {
			throw new IllegalArgumentException("선택 이벤트(IS_CHOICE = Y)에만 선택지를 추가할 수 있어요.");
		}

		c.setEventId(eventId);
		normalizeChoice(c);

		if (c.getChoiceOrder() == null) {
			c.setChoiceOrder(aDAO.selectNextChoiceOrder(eventId));
		} else {
			checkRange(c.getChoiceOrder(), 1, 99, "선택지 순서");
			if (aDAO.countChoiceOrder(eventId, c.getChoiceOrder()) > 0) {
				throw new IllegalArgumentException("이 이벤트에 이미 " + c.getChoiceOrder() + "번 선택지가 있어요.");
			}
		}

		validateChoice(c);
		aDAO.insertChoice(c);
		return c;
	}

	// 선택지 수정: 없는 선택지면 false → 404
	// 이벤트 ID·순서는 DB 값 그대로 (경험 ID = 이벤트ID_순서 라서 바꾸면 안 됨)
	public boolean updateChoice(int choiceId, EventChoiceDTO c) {
		EventChoiceDTO old = eDAO.selectChoice(choiceId);
		if (old == null) {
			return false;
		}
		c.setChoiceId(choiceId);
		c.setEventId(old.getEventId());
		c.setChoiceOrder(old.getChoiceOrder());
		normalizeChoice(c);
		validateChoice(c);
		return aDAO.updateChoice(c) == 1;
	}

	private void normalizeChoice(EventChoiceDTO c) {
		c.setChoiceText(blankToNull(c.getChoiceText()));
		c.setResultText(blankToNull(c.getResultText()));
		c.setResultAction(blankToNull(c.getResultAction()));
		c.setMigrateTo(blankToNull(c.getMigrateTo()));
	}

	private void validateChoice(EventChoiceDTO c) {
		if (c.getChoiceText() == null || c.getResultText() == null) {
			throw new IllegalArgumentException("선택지 문구와 결과 문구를 입력해 주세요.");
		}
		checkBytes(c.getChoiceText(), 200, "선택지 문구");
		checkBytes(c.getResultText(), 1000, "결과 문구");

		checkRange(c.getEffHealth(), -100, 100, "건강 효과");
		checkRange(c.getEffHappiness(), -100, 100, "행복 효과");
		checkRange(c.getEffKnowledge(), -100, 100, "지식 효과");
		checkRange(c.getEffRelation(), -100, 100, "관계 효과");
		if (c.getEffIncomeRate() != null && Math.abs(c.getEffIncomeRate()) > 99.999) {
			throw new IllegalArgumentException("소득 변화율이 너무 커요.");
		}
		if (c.getEffWealthRate() != null && Math.abs(c.getEffWealthRate()) > 999.99) {
			throw new IllegalArgumentException("재산 변화율이 너무 커요.");
		}

		if (c.getResultAction() != null && !aDAO.selectResultActions().contains(c.getResultAction())) {
			throw new IllegalArgumentException("결과 동작은 " + aDAO.selectResultActions() + " 중 하나예요.");
		}
		if (c.getMigrateTo() != null && !"MIGRATE".equals(c.getResultAction())) {
			throw new IllegalArgumentException("이민 나라는 결과 동작이 MIGRATE 일 때만 넣어요.");
		}
		if (c.getSuccessRate() != null && (c.getSuccessRate() < 0 || c.getSuccessRate() > 1)) {
			throw new IllegalArgumentException("성공 확률은 0~1 이에요.");
		}
	}

	// ================= 템플릿 확률 조정 =================

	// 템플릿 목록 (캐시가 아니라 DB 에서 바로)
	public List<EventTemplateDTO> templates() {
		return eDAO.selectTemplates();
	}

	// 확률·나이 수정 → 캐시 비우기 (이 서버는 재시작 없이 반영)
	// 없는 템플릿이면 null → 404
	public EventTemplateDTO updateTemplate(String templateId, EventTemplateDTO t) {
		if (aDAO.selectTemplate(templateId) == null) {
			return null;
		}
		t.setTemplateId(templateId);

		if (t.getBaseProb() != null && (t.getBaseProb() < 0 || t.getBaseProb() > 1)) {
			throw new IllegalArgumentException("기본 확률은 0~1 이에요.");
		}
		checkRange(t.getMinAge(), 0, 120, "최소 나이");
		checkRange(t.getMaxAge(), 0, 120, "최대 나이");
		if (t.getMinAge() != null && t.getMaxAge() != null && t.getMinAge() > t.getMaxAge()) {
			throw new IllegalArgumentException("최소 나이가 최대 나이보다 클 수 없어요.");
		}

		aDAO.updateTemplateProb(t);
		eServ.clearTemplateCache();
		return aDAO.selectTemplate(templateId); // 저장된 값 그대로 돌려주기
	}
	
	// 빈 문자열·공백 → null (선택 입력 칸)
	private void normalize(GameEventDTO e) {
		e.setTemplateId(blankToNull(e.getTemplateId()));
		e.setVarValue(blankToNull(e.getVarValue()));
		e.setHistEventId(blankToNull(e.getHistEventId()));
		e.setHistTypeCode(blankToNull(e.getHistTypeCode()));
		e.setTitle(blankToNull(e.getTitle()));
		e.setBaseText(blankToNull(e.getBaseText()));
		e.setGender(blankToNull(e.getGender()));
		e.setRequiredExpId(blankToNull(e.getRequiredExpId()));
		e.setExcludedExpId(blankToNull(e.getExcludedExpId()));
	}

	// DB 제약조건(CHECK·NOT NULL·컬럼 크기)을 먼저 검사 → 틀리면 IllegalArgumentException (400)
	private void validate(GameEventDTO e) {
		if ("LIFE".equals(e.getEventType())) {
			if (e.getTemplateId() == null) {
				throw new IllegalArgumentException("인생 이벤트는 템플릿 ID가 필요해요.");
			}
		} else {
			if (e.getHistEventId() == null || e.getHistTypeCode() == null || e.getIntensity() == null) {
				throw new IllegalArgumentException("역사 이벤트는 역사 사건 ID, 사건 유형, 강도가 필요해요.");
			}
		}

		if (e.getTitle() == null) {
			throw new IllegalArgumentException("제목을 입력해 주세요.");
		}
		checkBytes(e.getTitle(), 100, "제목");
		checkBytes(e.getBaseText(), 1000, "기본 문구");
		checkBytes(e.getVarValue(), 100, "변수 값");
		checkBytes(e.getRequiredExpId(), 50, "필요 경험 ID");
		checkBytes(e.getExcludedExpId(), 50, "제외 경험 ID");

		if (e.getEffectScale() == null || e.getEffectScale() < 0 || e.getEffectScale() > 99.99) {
			throw new IllegalArgumentException("효과 배율은 0~99.99 예요.");
		}
		checkRange(e.getIntensity(), 1, 3, "강도");
		checkRange(e.getMinAge(), 0, 120, "최소 나이");
		checkRange(e.getMaxAge(), 0, 120, "최대 나이");
		if (e.getMinAge() != null && e.getMaxAge() != null && e.getMinAge() > e.getMaxAge()) {
			throw new IllegalArgumentException("최소 나이가 최대 나이보다 클 수 없어요.");
		}
		checkRange(e.getMinWealth(), 1, 5, "최소 가정형편");
		checkRange(e.getMaxWealth(), 1, 5, "최대 가정형편");
		if (e.getMinWealth() != null && e.getMaxWealth() != null && e.getMinWealth() > e.getMaxWealth()) {
			throw new IllegalArgumentException("최소 가정형편이 최대보다 클 수 없어요.");
		}
		if (e.getGender() != null && !"M".equals(e.getGender()) && !"F".equals(e.getGender())) {
			throw new IllegalArgumentException("성별은 M, F 또는 빈칸이에요.");
		}

		checkYN(e.getIsOnce(), "한 번만");
		checkYN(e.getIsChoice(), "선택 이벤트");
		checkYN(e.getAiNarrate(), "AI 서술");
		checkYN(e.getIsReviewed(), "검수");

		if (e.getProbability() != null && (e.getProbability() < 0 || e.getProbability() > 1)) {
			throw new IllegalArgumentException("확률은 0~1 이에요.");
		}
		if (e.getEffWealthRate() != null && Math.abs(e.getEffWealthRate()) > 999.99) {
			throw new IllegalArgumentException("재산 변화율이 너무 커요.");
		}
	}

	private void checkBytes(String s, int max, String name) {
		if (s != null && s.getBytes(StandardCharsets.UTF_8).length > max) {
			throw new IllegalArgumentException(name + "이(가) 너무 길어요. (한글 약 " + (max / 3) + "자까지)");
		}
	}

	private void checkRange(Integer v, int min, int max, String name) {
		if (v != null && (v < min || v > max)) {
			throw new IllegalArgumentException(name + "은(는) " + min + "~" + max + " 예요.");
		}
	}

	private void checkYN(String v, String name) {
		if (!"Y".equals(v) && !"N".equals(v)) {
			throw new IllegalArgumentException(name + " 값은 Y 또는 N 이에요.");
		}
	}

	private String blankToNull(String s) {
		return (s == null || s.isBlank()) ? null : s.trim();
	}
}