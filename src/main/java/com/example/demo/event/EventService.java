package com.example.demo.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.engine.EngineService;
import com.example.demo.engine.LifeDTO;
import com.example.demo.event.dto.DestinationDTO;
import com.example.demo.event.dto.EventChoiceDTO;
import com.example.demo.event.dto.EventResultDTO;
import com.example.demo.event.dto.EventTemplateDTO;
import com.example.demo.event.dto.GameEventDTO;
import com.example.demo.event.dto.HistEventTypeDTO;

@Service
public class EventService {

	private final EventDAO eDAO;
	private final NarrateService narrateService;

	public EventService(EventDAO eDAO, NarrateService narrateService) {
		this.eDAO = eDAO;
		this.narrateService = narrateService;
	}

	// ===== 1단계: 템플릿 목록 (한 번만 DB 조회) =====

	private List<EventTemplateDTO> templateCache;

	public List<EventTemplateDTO> getTemplates() {
		if (templateCache == null) {
			templateCache = eDAO.selectTemplates();
		}
		return templateCache;
	}

	public void clearTemplateCache() {
		templateCache = null;
	}

	// ===== 2단계: 템플릿별 확률 계산 + 굴리기 =====

	// 올해 걸린 템플릿 목록
	public List<EventTemplateDTO> rollTemplates(int age, int year, String countryCode, String gender,
			Map<String, Double> traits) {
		Map<String, Object> stat = eDAO.selectCountryStat(countryCode, year / 10 * 10);
		List<EventTemplateDTO> hits = new ArrayList<>();

		for (EventTemplateDTO t : getTemplates()) {
			if (age < t.getMinAge() || age > t.getMaxAge())
				continue; // 나이 조건

			double prob = finalProbability(t, age, stat, countryCode, gender, traits);
			if (Math.random() < prob) {
				hits.add(t);
			}
		}
		return hits;
	}

	private static final int AVG_MARRY_AGE = 30; // 평균 결혼 나이 (설계값, 추후 시뮬레이션 결과로 조정)


	// 나라별 인구 10만 명당 수감자 수 (원작 PRISON_RATE, World Prison Brief), 없는 나라는 140
	private static final Map<String, Integer> PRISON_RATE = Map.of("THA", 400, "TUR", 380, "IRN", 230,
			"CHN", 120, "KOR", 105, "BGD", 45, "IND", 40, "PAK", 40, "JPN", 35);

	// 나라 수감률 배율 = √(수감률 ÷ 140) (예: 한국 0.87, 일본 0.5, 태국 1.69)
	private double prisonK(String countryCode) {
		return Math.sqrt(PRISON_RATE.getOrDefault(countryCode, 140) / 140.0);
	}

	// 위험한 제안을 받아들였을 때 붙잡힐 확률 = 35% × 나라 배율, 12~70% (한국 약 30%)
	private double caughtRate(String countryCode) {
		return Math.min(Math.max(0.35 * prisonK(countryCode), 0.12), 0.7);
	}
	
	// 템플릿 하나의 올해 발생 확률
	public double calcProbability(EventTemplateDTO t, int age, Map<String, Object> stat, String countryCode,
			String gender) {
		double base = t.getBaseProb() == null ? 0 : t.getBaseProb();

		switch (t.getProbMethod()) {
		case "TFR": // 출산: 합계출산율 ÷ 결혼 후 출산 가능 기간 (평균 결혼 나이 ~ 템플릿 최대 나이)
			Double tfr = getStat(stat, "TFR");
			int years = t.getMaxAge() - AVG_MARRY_AGE + 1;
			return tfr == null ? base : tfr / years;
		case "UNEMPLOYMENT": // 실직: 실업률 ÷ 100 × 0.4 (실업률은 '지금 일자리 없는 비율'이라 1년 실직 확률보다 높음)
			Double unemp = getStat(stat, "UNEMPLOYMENT_RATE");
			return unemp == null ? base : unemp / 100 * 0.4;
		case "NET_MIGRATION": // 이민: 기본 확률 × (1 - 순이민률‰), 배율 0.5~2.0배 제한 (유출 많을수록 ↑)
			Double mig = getStat(stat, "NET_MIGRATION_RATE");
			if (mig == null)
				return base; // 통계 없으면 기본 확률
			double migRate = Math.min(Math.max(1 - mig, 0.5), 2.0); // 바닥 0.5: 유입국도 최소 절반 / 천장 2.0: 원작 이민 비율(1.2%)에 맞춤
			return base * migRate;
		case "DEATH_CAUSE": // 질병: 기본 확률 × 나이대 배율 × 나라 건강 배율
			Double le = getStat(stat, "LIFE_EXPECTANCY");
			// 나라 건강 배율 = 75(기준 기대수명) ÷ 기대수명, 0.8~2.0배 제한 / 통계 없으면 1.0
			double countryRate = (le == null) ? 1.0 : Math.min(Math.max(75 / le, 0.8), 2.0);
			return base * diseaseAgeRate(age) * countryRate;
		case "SPOUSE_DEATH": // 사별: 배우자(반대 성별, 같은 나이)의 올해 사망 확률 = 엔진 원작 생명표 (C07)
			String spouseGender = "M".equals(gender) ? "F" : "M";
			return EngineService.baseDeathProb(countryCode, getStat(stat, "LIFE_EXPECTANCY"),
					getStat(stat, "INFANT_MORTALITY"), getStat(stat, "U5_MORTALITY"), spouseGender, age);
		case "CRIME": // 위험한 제안: 기본 확률 × 성별(여성 0.08배) × 나라 수감률 배율 (원작 crime)
			return base * ("F".equals(gender) ? 0.08 : 1) * prisonK(countryCode);
		default: // FIXED
			return base;
		}
	}

	// 발생 확률에 영향을 주는 특성 배율 (질병·연애). 투자는 성공 확률이라 applyChoice에서 적용
	private double occurTraitRate(EventTemplateDTO t, Map<String, Double> traits) {
		if (traits == null || t.getTraitId() == null)
			return 1.0;
		String id = t.getTraitId();
		if (!"DISEASE".equals(id) && !"LOVE".equals(id))
			return 1.0;
		return 1 + traits.getOrDefault(id, 0.0); // 예: 질병 Lv.5 → 1 + (-0.4) = 0.6배
	}

	// 최종 확률 = 통계 기반 확률 × 특성 배율
	public double finalProbability(EventTemplateDTO t, int age, Map<String, Object> stat, String countryCode,
			String gender, Map<String, Double> traits) {
		return calcProbability(t, age, stat, countryCode, gender) * occurTraitRate(t, traits);
	}

	// 질병 나이대 배율
	private double diseaseAgeRate(int age) {
		if (age < 5)
			return 2.0;
		if (age < 15)
			return 1.0;
		if (age < 50)
			return 0.8;
		if (age < 70)
			return 1.5;
		return 2.5;
	}

	// 통계 값 꺼내기 (없으면 null → BASE_PROB 사용)
	private Double getStat(Map<String, Object> stat, String key) {
		if (stat == null || stat.get(key) == null)
			return null;
		return ((Number) stat.get(key)).doubleValue();
	}

	// ===== 3단계: 걸린 템플릿 안에서 이벤트 하나 고르기 =====

	public GameEventDTO pickEvent(EventTemplateDTO t, int age, String gender, int wealth, List<String> expIds) {
		List<GameEventDTO> candidates = new ArrayList<>();

		for (GameEventDTO e : eDAO.selectLifeCandidates(t.getTemplateId(), age, gender, wealth)) {
			if (passExp(e, expIds)) {
				candidates.add(e);
			}
		}
		if (candidates.isEmpty())
			return null; // 조건 맞는 이벤트 없음 → 그해는 없던 걸로

		int idx = (int) (Math.random() * candidates.size());
		return candidates.get(idx);
	}

	// 경험 조건 통과 여부
	private boolean passExp(GameEventDTO e, List<String> expIds) {
		// 1. 필요 경험이 있는데 내가 안 겪었으면 탈락 (예: 결혼 안 했는데 첫째 출산)
		if (e.getRequiredExpId() != null && !expIds.contains(e.getRequiredExpId())) {
			return false;
		}
		// 2. 제외 경험을 내가 겪었으면 탈락 (예: 결혼했는데 새로운 만남)
		if (e.getExcludedExpId() != null && expIds.contains(e.getExcludedExpId())) {
			return false;
		}
		// 3. 한 번만 일어나는 이벤트인데 이미 겪었으면 탈락
		if ("Y".equals(e.getIsOnce())) {
			for (String exp : expIds) {
				if (exp.equals(e.getEventId()) || exp.startsWith(e.getEventId() + "_")) {
					return false;
				}
			}
		}
		return true;
	}

	// 최근에 이 선택 이벤트를 물어봤는지 (선택 이벤트 경험 ID = 이벤트ID_선택지순서)
	private boolean askedRecently(String eventId, List<String> recentIds) {
		for (String id : recentIds) {
			if (id.startsWith(eventId + "_"))
				return true;
		}
		return false;
	}

	// 부모님 별세 이벤트 → 엔진 결과 동작 (템플릿 동작 PARENT_DEATH 대신 누가 떠나셨는지 알려 줌)
	// CAR·FIRE 는 사고로 함께 별세 (내 이벤트가 낮은 확률로 굴림)
	private static final Map<String, String> PARENT_ACTION = Map.of("LE_PARENT_DEATH_MOTHER", "PARENT_DEATH_MOTHER",
			"LE_PARENT_DEATH_FATHER", "PARENT_DEATH_FATHER", "LE_PARENT_DEATH_SAMEYEAR", "PARENT_DEATH_BOTH",
			"LE_PARENT_DEATH_CAR", "PARENT_DEATH_BOTH", "LE_PARENT_DEATH_FIRE", "PARENT_DEATH_BOTH");

	// ===== 4단계: 문장 만들기 + 효과 계산 =====

	// 고른 이벤트 → 결과 (문장 + 효과)
	public EventResultDTO buildResult(EventTemplateDTO t, GameEventDTO e, String name, int age, int year,
			String countryCode) {
		EventResultDTO r = new EventResultDTO();
		r.setEventId(e.getEventId());
		r.setExpId(e.getEventId());
		r.setEventType(e.getEventType());
		r.setTitle(e.getTitle());
		r.setAiNarrate("Y".equals(e.getAiNarrate()));
		r.setChoice("Y".equals(e.getIsChoice()));
		if (r.isChoice()) {
			r.setChoices(eDAO.selectChoices(e.getEventId()));
		}

		// 1. 문장: 이벤트 전용 문장(BASE_TEXT)이 있으면 그것, 없으면 템플릿 문장 2~3개 중 하나를 랜덤으로
		// (예: 화재는 사고 템플릿 문장 '몇 달을 쉬어야 했다·크게 다쳤다'가 안 맞아서 전용 문장 사용)
		String sentence;
		if (e.getBaseText() != null) {
			sentence = e.getBaseText();
		} else {
			List<String> texts = eDAO.selectTexts(t.getTemplateId());
			sentence = texts.isEmpty() ? e.getTitle() : texts.get((int) (Math.random() * texts.size()));
		}

		// 2. 변수 치환
		String countryName = eDAO.selectCountryName(countryCode);
		sentence = sentence.replace("{이름}", name).replace("{나이}", String.valueOf(age))
				.replace("{연도}", String.valueOf(year)).replace("{나라}", countryName == null ? countryCode : countryName)
				.replace("{변수}", e.getVarValue() == null ? "" : e.getVarValue());
		r.setLogText(Josa.apply(sentence));

		// 3. 효과 = 템플릿 효과 × 이벤트 배율 (예: 질병 건강 -10 × 결핵 1.5 = -15)
		double scale = e.getEffectScale() == null ? 1.0 : e.getEffectScale();
		r.setEffHealth((int) Math.round(nz(t.getEffHealth()) * scale));
		r.setEffHappiness((int) Math.round(nz(t.getEffHappiness()) * scale));
		r.setEffKnowledge((int) Math.round(nz(t.getEffKnowledge()) * scale));
		r.setEffRelation((int) Math.round(nz(t.getEffRelation()) * scale));

		// 자산: 이벤트에 값이 있으면 그 값 그대로(소득 변화 0), 없으면 템플릿 × 배율
		if (e.getEffWealthRate() != null) {
			r.setEffWealthRate(round3(e.getEffWealthRate()));
			r.setEffIncomeRate(0);
		} else {
			r.setEffIncomeRate(round3(nz(t.getEffIncomeRate()) * scale));
			r.setEffWealthRate(round3(nz(t.getEffWealthRate()) * scale));
		}
		r.setResultAction(PARENT_ACTION.getOrDefault(e.getEventId(), t.getResultAction())); // 부모 별세는 누가 떠나셨는지

		return r;
	}

	// null이면 0
	private double nz(Number n) {
		return n == null ? 0 : n.doubleValue();
	}

	// 소수점 3자리 반올림 (0.30000000000000004 → 0.3)
	private double round3(double v) {
		return Math.round(v * 1000) / 1000.0;
	}

	// 지금 처지를 나타내는 메모리 전용 경험 (DB 저장 안 함, REQUIRED·EXCLUDED 조건 판정용)
	// STATE_MARRIED 결혼 중 / STATE_ABROAD 태어난 나라 밖 / STATE_STUDENT 재학 중 /
	// STATE_EMPLOYED 취업 중
	// STATE_TROPICAL 남·동남아시아(말라리아) / STATE_DATING 사귀는 중 / STATE_SINGLE 미혼이고 사귀는 사람
	// 없음
	public List<String> stateExps(LifeDTO life) {
		List<String> states = new ArrayList<>();
		boolean married = "Y".equals(life.getMarriedYn());
		if (married) {
			states.add("STATE_MARRIED");
		}
		if (life.getBirthCountry() != null && !life.getBirthCountry().equals(life.getCurrentCountry())) {
			states.add("STATE_ABROAD");
		}
		if ("ENROLLED".equals(life.getSchoolStatus())) {
			states.add("STATE_STUDENT"); // 장학금
		}
		if ("EMPLOYED".equals(life.getJobStatus())) {
			states.add("STATE_EMPLOYED"); // 직장 표창
		}
		String region = eDAO.selectRegionCode(life.getCurrentCountry());
		if ("SOUTH".equals(region) || "SOUTHEAST".equals(region)) {
			states.add("STATE_TROPICAL"); // 말라리아
		}
		if (!married) {
			int dating = datingYears(life);
			states.add(dating >= 0 ? "STATE_DATING" : "STATE_SINGLE"); // 만남 / 이별·청혼
			if (dating >= 2) {
				states.add("STATE_DATING_LONG"); // 사귄 지 2년 이상 → 결혼 이야기
			}
		}
		if (life.getChildCount() > 0) {
			states.add("STATE_HAS_CHILD"); // 자녀가 있음 → 아이가 크게 아픔
		}
		// 형편이 어려움: 일자리가 없거나(실직·18세 넘어 아직 취업 전) 빚이 있음 → 위험한 제안
		boolean jobless = "UNEMPLOYED".equals(life.getJobStatus())
				|| (life.getJobStatus() == null && life.getCurrentAge() >= 18);
		if (jobless || life.getAssets() < 0) {
			states.add("STATE_HARDUP");
		}
		if (bothParents(life)) {
			states.add("STATE_BOTH_PARENTS"); // 부모님 두 분 다 살아 계심 (사고로 함께 별세 조건)
		}
		return states;
	}

	// 부모님 두 분 다 살아 계심: 부모 생존이 N이 아니고, 먼저 떠나신 분 표시(PAR-M-, PAR-F-)도 없음
	private boolean bothParents(LifeDTO life) {
		if ("N".equals(life.getParentAlive())) {
			return false;
		}
		String owns = life.getOwns() == null ? "" : life.getOwns();
		return !owns.contains("PAR-M-") && !owns.contains("PAR-F-");
	}

	// ===== 부모님 별세 (엔진 parentYear 가 famSeed 수명으로 그해 떠나실 분을 정하면 호출) =====
	// eventId: LE_PARENT_DEATH_MOTHER / LE_PARENT_DEATH_FATHER /
	// LE_PARENT_DEATH_SAMEYEAR
	// 결과 동작은 PARENT_ACTION 표대로, 이벤트가 없으면 null (엔진은 임시 문장으로 처리)
	public EventResultDTO parentDeathEvent(LifeDTO life, String eventId) {
		GameEventDTO e = eDAO.selectEvent(eventId);
		if (e == null) {
			return null;
		}
		EventTemplateDTO t = findTemplate(e.getTemplateId());
		if (t == null) {
			return null;
		}
		int age = life.getCurrentAge();
		int year = life.getBirthYear() + age;
		EventResultDTO r = buildResult(t, e, life.getName(), age, year, life.getCurrentCountry());

		// 한 분이 먼저 떠나셨으면 '~에 이어 ~마저' 문장으로 (엔진이 OWNS 에 PAR-M-·PAR-F- 저장)
		String owns = life.getOwns() == null ? "" : life.getOwns();
		if ("LE_PARENT_DEATH_MOTHER".equals(eventId) && owns.contains("PAR-F-")) {
			r.setLogText(year + "년, 아버지에 이어 어머니마저 세상을 떠나셨다.");
		} else if ("LE_PARENT_DEATH_FATHER".equals(eventId) && owns.contains("PAR-M-")) {
			r.setLogText(year + "년, 어머니에 이어 아버지마저 세상을 떠나셨다.");
		}

		narrateService.narrate(life, r, age, year); // AI 서술 대상이면 문장 다듬기 (실패하면 위 문장 그대로)
		return r;
	}

	// 템플릿 ID로 템플릿 찾기 (캐시에서)
	private EventTemplateDTO findTemplate(String templateId) {
		for (EventTemplateDTO t : getTemplates()) {
			if (t.getTemplateId().equals(templateId)) {
				return t;
			}
		}
		return null;
	}

	// 사귄 햇수: 사귀는 중이면 지금 나이 - 마지막 만남 나이, 사귀는 중이 아니면 -1
	// 사귀는 중 = 마지막 만남이 그 뒤의 이별·청혼·이혼·사별·결혼 위기·결혼 이야기 헤어짐보다 나중
	// (청혼을 거절했거나 결혼이 끝났으면 그 만남은 끝난 것)
	private int datingYears(LifeDTO life) {
		Map<String, Object> ages = eDAO.selectLoveAges(life.getLifeId());
		if (ages == null || ages.get("MEET_AGE") == null) {
			return -1; // 만난 적 없음
		}
		int meet = ((Number) ages.get("MEET_AGE")).intValue();
		Object end = ages.get("END_AGE");
		if (end != null && meet <= ((Number) end).intValue()) {
			return -1; // 마지막 만남이 이미 끝남
		}
		return Math.max(0, life.getCurrentAge() - meet);
	}

	// ===== 엔진 연결: 1년치 이벤트 굴리기 (EngineService가 호출) =====
	public List<EventResultDTO> rollYear(LifeDTO life) {
		if ("PRISON".equals(life.getJobStatus())) {
			return new ArrayList<>(); // 교도소에 있는 동안은 이벤트 없음 (엔진이 교도소 생활 처리)
		}
		int age = life.getCurrentAge();
		int year = life.getBirthYear() + age;
		List<String> expIds = eDAO.selectExpIds(life.getLifeId());
		Map<String, Double> traits = getTraitEffects(life.getLifeId()); // C13 특성 효과 (없으면 Lv.1)
		expIds.addAll(stateExps(life)); // 지금 처지 (결혼·이민·학생·직장인·열대·연애, DB 저장 안 함)
		// 재혼하면 자녀 계획 다시 묻기: 마지막 결혼보다 먼저 정한 자녀 계획은 지난 결혼 것이라 판정에서 뺌
		Integer marryAge = eDAO.selectLastExpAge(life.getLifeId(), "LE_LOVE_PROPOSAL_1");
		Integer planAge = eDAO.selectLastExpAge(life.getLifeId(), "LE_CHILD_PLAN_%");
		if (marryAge != null && planAge != null && planAge < marryAge) {
			expIds.removeIf(id -> id.startsWith("LE_CHILD_PLAN_")); // 순회 중 삭제를 안전하게 처리
		}
		List<EventResultDTO> results = new ArrayList<>();
		boolean hasChoice = false; // 한 해에 선택 이벤트는 1개만 (역사 + 인생 합쳐서)
		List<String> recentIds = eDAO.selectRecentExpIds(life.getLifeId(), age - 5); // 최근 5년 경험

		// 1. 역사 이벤트 (C10): 먼저 굴림
		for (EventResultDTO h : rollHist(life.getCurrentCountry(), year, age, life.getGender(), life.getName(),
				expIds)) {
			if (h.isChoice()) {
				if (hasChoice)
					continue;
				hasChoice = true;
			}
			results.add(h);
		}

		// 2. 인생 이벤트 (C09)
		for (EventTemplateDTO t : rollTemplates(age, year, life.getCurrentCountry(), life.getGender(), traits)) {
			GameEventDTO e = pickEvent(t, age, life.getGender(), life.getWealthClass(), expIds);
			if (e == null)
				continue; // 조건 맞는 이벤트 없음
			if ("Y".equals(e.getIsChoice())) {
				if (hasChoice || askedRecently(e.getEventId(), recentIds))
					continue; // 이미 선택 이벤트가 있거나, 최근 5년 안에 물어본 이벤트면 건너뜀
				hasChoice = true;
			}

			if ("LE_LOVE_WEDTALK".equals(e.getEventId())) {
				e.setVarValue(String.valueOf(datingYears(life))); // 결혼 이야기: 사귄 햇수 → 문장의 {변수}
			}
			results.add(buildResult(t, e, life.getName(), age, year, life.getCurrentCountry()));
		}

		// 3. AI 서술 (C11): AI 서술 대상만, 실패하면 기본 문구 그대로
		for (EventResultDTO r : results) {
			narrateService.narrate(life, r, age, year);
		}

		return results;
	}

	// ===== 5단계: 선택지를 고르면 결과 만들기 =====

	public EventResultDTO applyChoice(LifeDTO life, int choiceId, int age) {
		EventChoiceDTO c = eDAO.selectChoice(choiceId);
		if (c == null)
			return null; // 없는 선택지

		int year = life.getBirthYear() + age;

		EventResultDTO r = new EventResultDTO();
		r.setEventId(c.getEventId());
		r.setExpId(c.getEventId() + "_" + c.getChoiceOrder()); // 예: LE_MIGRATE_WORK_1
		r.setEventType(c.getEventId().startsWith("LE_") ? "LIFE" : "HIST"); // 인생 이벤트 ID는 LE_로 시작
		r.setTitle(c.getChoiceText());

		// 1. 결과 문구 변수 치환
		String countryName = eDAO.selectCountryName(life.getCurrentCountry());
		String text = c.getResultText().replace("{이름}", life.getName()).replace("{나이}", String.valueOf(age))
				.replace("{연도}", String.valueOf(year))
				.replace("{나라}", countryName == null ? life.getCurrentCountry() : countryName);

		// 2. 효과 (선택지 값 그대로)
		r.setEffHealth((int) nz(c.getEffHealth()));
		r.setEffHappiness((int) nz(c.getEffHappiness()));
		r.setEffKnowledge((int) nz(c.getEffKnowledge()));
		r.setEffRelation((int) nz(c.getEffRelation()));
		r.setEffIncomeRate(round3(nz(c.getEffIncomeRate())));
		double wealth = round3(nz(c.getEffWealthRate()));

		// 3. 성공·실패가 있는 선택지 (투자·치료·참여·상담): SUCCESS_RATE로 판정
		if (c.getSuccessRate() != null) {
			String group = actionGroup(c.getResultAction()); // TREAT_HOSPITAL → TREAT
			// 위험한 제안은 '안 붙잡힐 확률'을 나라 수감률로 계산 (DB SUCCESS_RATE 값은 쓰지 않음)
			double rate = "CRIME".equals(group) ? 1 - caughtRate(life.getCurrentCountry())
					: Math.min(c.getSuccessRate() * traitRate(group, life.getLifeId()), 0.95);
			boolean success = Math.random() < rate;
			text = text + (success ? SUCCESS_TEXT.get(group)[0] : SUCCESS_TEXT.get(group)[1]);
			if (!success) {
				wealth = applyFail(group, r, wealth); // 종류별 실패 효과
				if ("CRIME".equals(group)) {
					text = text + " 징역 " + r.getPrisonYears() + "년을 선고받았다.";
				}
			}
			r.setResultAction(afterAction(c.getResultAction(), success)); // 엔진에 보낼 상태 변화
		} else {
			r.setResultAction(c.getResultAction()); // MARRY, MIGRATE 등

			if ("RETURN_HOME".equals(c.getResultAction())) {
				// 고향으로 돌아가기: 엔진에는 MIGRATE + 목적지 = 태어난 나라로 보냄
				r.setResultAction("MIGRATE");
				r.setMigrateTo(life.getBirthCountry());
			} else if ("MIGRATE".equals(c.getResultAction()) && c.getMigrateTo() == null) {
				// 떠난다(MIGRATE)인데 목적지가 정해지지 않았으면 → 목적지 후보 5곳
				r.setNeedDestination(true);
				r.setDestinations(pickDestinations(life.getCurrentCountry(), year));
			}
		}

		r.setEffWealthRate(wealth);
		if (r.getMigrateTo() == null) {
			r.setMigrateTo(c.getMigrateTo()); // 귀향은 위에서 태어난 나라를 넣었으니 덮어쓰지 않음
		}
		r.setLogText(Josa.apply(text));

		return r;
	}

	// ===== 성공·실패 선택지 (투자·치료·참여·상담) =====

	// 종류별 결과 문장 [성공, 실패]
	private static final Map<String, String[]> SUCCESS_TEXT = Map.of("INVEST",
			new String[] { " 투자는 성공적이었다.", " 하지만 투자는 실패해 손해를 봤다." }, "PROTEST",
			new String[] { " 함께한 사람들과 변화를 이끌어 냈다.", " 하지만 진압 과정에서 크게 다쳤다." }, "ENLIST",
			new String[] { " 무사히 살아 돌아왔다.", " 하지만 전투 중 크게 다쳤다." }, "COUNSEL",
			new String[] { " 서로를 다시 이해하게 되었다.", " 하지만 끝내 마음을 돌리지 못하고 헤어졌다." }, "TREAT",
			new String[] { " 치료가 잘 되어 몸을 회복했다.", " 하지만 치료가 듣지 않아 병이 깊어졌다." }, "KIDTREAT",
			new String[] { " 다행히 아이는 무사히 나았다.", " 하지만 아이는 오래 앓으며 크게 고생했다." }, "CRIME", new String[] {
					" 약속한 큰돈은 끝내 들어오지 않았다. 손에 쥔 건 몇 주 치 벌이뿐이었고, 그날 이후 문 두드리는 소리에 자주 잠을 깼다.", " 하지만 붙잡혀 재판을 받았다." });

	// 결과 종류 묶기: TREAT_HOSPITAL, TREAT_HOME, TREAT_LOAN → TREAT / KIDTREAT_* →
	// KIDTREAT (아이 치료)
	private String actionGroup(String action) {
		if (action == null) {
			return null;
		}
		if (action.startsWith("TREAT_")) {
			return "TREAT";
		}
		return action.startsWith("KIDTREAT_") ? "KIDTREAT" : action;
	}

	// 특성 배율: 투자 → 투자 특성 (Lv.5 × 1.4), 치료 → 질병 특성 (Lv.5 × 1.4), 나머지 1
	private double traitRate(String group, int lifeId) {
		if ("INVEST".equals(group))
			return 1 + getTraitEffect(lifeId, "INVEST");
		if ("TREAT".equals(group))
			return 1 - getTraitEffect(lifeId, "DISEASE"); // 질병 효과는 음수라 빼면 커짐
		if ("COUNSEL".equals(group))
			return 1 + getTraitEffect(lifeId, "LOVE"); // 상담 → 연애 특성 (관계를 잘 꾸리는 능력)
		return 1.0;
	}

	// 실패 효과 (설계값), 바뀐 재산 값을 돌려줌
	private double applyFail(String group, EventResultDTO r, double wealth) {
		switch (group) {
		case "INVEST": // 성공했을 때만큼 손해, 소득 증가 없음, 행복 감소
			r.setEffIncomeRate(0);
			r.setEffHappiness(-Math.abs(r.getEffHappiness()) - 3);
			return -wealth;
		case "PROTEST": // 진압으로 부상
			r.setEffHealth(-10);
			r.setEffHappiness(-6);
			r.setEffKnowledge(0);
			r.setEffRelation(0);
			r.setDeathAdd(0.02);
			r.setDeathCause("시위 진압");
			return wealth;
		case "ENLIST": // 전쟁 피해(선택지 효과)에 전투 중 중상을 더함, 명예(관계)는 그대로
			r.setEffHealth(r.getEffHealth() - 15);
			r.setEffHappiness(r.getEffHappiness() - 5);
			r.setDeathAdd(0.05);
			r.setDeathCause("전쟁");
			return wealth;
		case "COUNSEL": // 상담 실패 → 이혼 (기존 이혼 템플릿 효과와 같게)
			r.setEffHealth(-2);
			r.setEffHappiness(-10);
			r.setEffKnowledge(0);
			r.setEffRelation(-6);
			return -0.20;
		case "TREAT": // 치료 실패: 병이 깊어짐 (치료 방법과 관계없이 같은 피해)
			r.setEffHealth(-15);
			r.setEffHappiness(-8);
			r.setDeathAdd(0.05);
			r.setDeathCause("질병");
			return wealth; // 치료비는 성공·실패와 관계없이 나감
		case "KIDTREAT": // 아이 치료 실패: 아이가 오래 앓음 → 마음고생 (내 건강·사망 확률은 그대로)
			r.setEffHappiness(r.getEffHappiness() - 7);
			r.setEffRelation(r.getEffRelation() - 2);
			return wealth; // 치료비는 성공·실패와 관계없이 나감
		case "CRIME": // 위험한 제안에서 붙잡힘: 징역 1~4년 (원작 crime)
			r.setEffHappiness(-15);
			r.setEffRelation(-10);
			r.setPrisonYears(1 + (int) (Math.random() * 4));
			return wealth;
		default:
			return wealth;
		}
	}

	// 판정 후 엔진에 보낼 상태 변화
	private String afterAction(String action, boolean success) {
		if ("COUNSEL".equals(action) && !success)
			return "MARRIAGE_END"; // 상담 실패 → 이혼 (엔진이 결혼 상태 해제)
		if ("TREAT_LOAN".equals(action) || "KIDTREAT_LOAN".equals(action))
			return "LOAN"; // 빚내서 치료 (나·아이) → 성공·실패와 관계없이 빚 (엔진이 재산에서 뺌)
		if ("CRIME".equals(action))
			return success ? "CRIME_PAY" : "PRISON"; // 위험한 제안: 안 붙잡힘 → 몇 주 치 벌이 / 붙잡힘 → 교도소 (엔진)
		return null; // 투자·참여·병원·집 치료는 상태 변화 없음
	}

	// ===== 이민 목적지 (C10) =====

	private static final String[] TIER_NAMES = { "상위", "중상위", "중위", "중하위", "하위" };

	// 목적지 후보: GDP 순 정렬 → 5구역으로 나눠 구역마다 1곳 랜덤 (후보 5곳 이상 기준)
	public List<DestinationDTO> pickDestinations(String currentCountry, int year) {
		List<DestinationDTO> all = eDAO.selectMigrateCandidates(currentCountry, year / 10 * 10);
		List<DestinationDTO> picks = new ArrayList<>();
		int n = all.size();

		for (int g = 0; g < 5; g++) {
			int start = g * n / 5; // 이 구역 시작 위치
			int end = (g + 1) * n / 5; // 이 구역 끝 위치 (포함 안 함)
			DestinationDTO d = all.get(start + (int) (Math.random() * (end - start)));
			d.setTier(g + 1);
			d.setTierName(TIER_NAMES[g]);
			picks.add(d);
		}
		return picks;
	}

	// 구역별 효과: 소득 배율(이민자 불이익) / 행복(생활 안정)
	private static final double[] TIER_INCOME = { -0.30, -0.20, -0.10, -0.05, 0 };
	private static final int[] TIER_HAPPY = { 2, 1, 0, -1, -2 };

	// 목적지를 고르면 결과: 구역 효과 + 나라 이동
	// eventId: 떠난다를 고른 이벤트 (기록용), age: 이벤트가 일어난 나이
	public EventResultDTO applyDestination(LifeDTO life, String countryCode, int age, String eventId) {
		int year = life.getBirthYear() + age;
		List<DestinationDTO> all = eDAO.selectMigrateCandidates(life.getCurrentCountry(), year / 10 * 10);

		// 1. 고른 나라가 후보에 있는지 + 몇 번째인지 (GDP 순위)
		int idx = -1;
		for (int i = 0; i < all.size(); i++) {
			if (all.get(i).getCountryCode().equals(countryCode)) {
				idx = i;
				break;
			}
		}
		if (idx == -1)
			return null; // 없는 나라이거나 지금 사는 나라

		DestinationDTO d = all.get(idx);
		int g = idx * 5 / all.size(); // 구역 0(상위) ~ 4(하위)

		// 2. 결과
		EventResultDTO r = new EventResultDTO();
		r.setEventId(eventId);
		r.setExpId("MIGRATE_" + countryCode); // 예: MIGRATE_JPN (어느 나라로 갔는지 경험)
		r.setEventType(eventId.startsWith("LE_") ? "LIFE" : "HIST");
		r.setTitle(d.getNameKo() + " 이주");
		r.setLogText(Josa.apply(year + "년, " + life.getName() + "은(는) " + d.getNameKo() + "(으)로 떠났다."));
		r.setEffIncomeRate(TIER_INCOME[g]);
		r.setEffHappiness(TIER_HAPPY[g]);
		r.setResultAction("MIGRATE");
		r.setMigrateTo(countryCode); // 엔진이 이 값으로 나라 변경
		return r;
	}

	// ===== 역사 이벤트 (C10) =====

	// 유형 5종 효과 (한 번만 DB 조회, 유형 코드로 찾기)
	private Map<String, HistEventTypeDTO> histTypeCache;

	private HistEventTypeDTO getHistType(String typeCode) {
		if (histTypeCache == null) {
			histTypeCache = new HashMap<>();
			for (HistEventTypeDTO h : eDAO.selectHistTypes()) {
				histTypeCache.put(h.getTypeCode(), h);
			}
		}
		return histTypeCache.get(typeCode);
	}

	// 올해 겪는 역사 이벤트 목록
	public List<EventResultDTO> rollHist(String countryCode, int year, int age, String gender, String name,
			List<String> expIds) {
		List<EventResultDTO> results = new ArrayList<>();
		for (GameEventDTO e : eDAO.selectHistCandidates(countryCode, year, age, gender)) {
			if (!passExp(e, expIds))
				continue; // 이미 겪은 사건 (IS_ONCE)
			double prob = e.getProbability() == null ? 0 : e.getProbability();
			if (Math.random() < prob) {
				results.add(buildHistResult(e, name, age, year, countryCode));
			}
		}
		return results;
	}

	// 역사 이벤트 결과: 효과 = 유형 효과 × 강도 / 재산은 이벤트 값 우선 (A 규칙)
	private EventResultDTO buildHistResult(GameEventDTO e, String name, int age, int year, String countryCode) {
		EventResultDTO r = new EventResultDTO();
		r.setEventId(e.getEventId());
		r.setExpId(e.getEventId());
		r.setEventType("HIST");
		r.setTitle(e.getTitle());
		r.setAiNarrate("Y".equals(e.getAiNarrate()));
		r.setChoice("Y".equals(e.getIsChoice()));
		if (r.isChoice()) {
			List<EventChoiceDTO> choices = eDAO.selectChoices(e.getEventId());
			if (canChoose(choices, age)) {
				r.setChoices(choices);
			} else {
				r.setChoice(false); // 선택할 나이가 아니면 일반 역사 이벤트로 (예: 3살에게 참전을 묻지 않음)
			}
		}

		// 1. 문장: 검수된 게임 문구, 없으면 제목
		String countryName = eDAO.selectCountryName(countryCode);
		String text = e.getBaseText() == null ? e.getTitle() : e.getBaseText();
		text = text.replace("{이름}", name).replace("{나이}", String.valueOf(age)).replace("{연도}", String.valueOf(year))
				.replace("{나라}", countryName == null ? countryCode : countryName);
		r.setLogText(Josa.apply(text));

		// 선택 이벤트는 효과 없이 대기 → 고른 선택지 효과만 반영
		if (r.isChoice()) {
			return r;
		}

		// 2. 효과: 유형 효과 × 강도 (예: 전쟁 행복 -10 × 강도 3 = -30)
		HistEventTypeDTO h = getHistType(e.getHistTypeCode());
		int k = e.getIntensity() == null ? 1 : e.getIntensity();
		if (h != null) {
			r.setEffHealth((int) Math.round(nz(h.getEffHealth()) * k));
			r.setEffHappiness((int) Math.round(nz(h.getEffHappiness()) * k));
			r.setEffKnowledge((int) Math.round(nz(h.getEffKnowledge()) * k));
			r.setEffRelation((int) Math.round(nz(h.getEffRelation()) * k));
			r.setDeathAdd(nz(h.getDeathAdd()) * k); // 그해 사망 확률 가산 (엔진 C07), 작은 값이라 반올림 안 함
			r.setDeathCause(h.getDeathCause());
		}

		// 3. 재산: 이벤트 값이 있으면 그 값 그대로, 없으면 유형 값 × 강도
		if (e.getEffWealthRate() != null) {
			r.setEffWealthRate(round3(e.getEffWealthRate()));
		} else if (h != null) {
			r.setEffWealthRate(round3(nz(h.getEffWealthRate()) * k));
		}
		r.setEffIncomeRate(0);

		return r;
	}

	// 선택 가능 나이: 시위 15세 이상, 참전·징집 18~45세, 나머지(피란 등)는 나이 제한 없음
	private boolean canChoose(List<EventChoiceDTO> choices, int age) {
		for (EventChoiceDTO c : choices) {
			if ("PROTEST".equals(c.getResultAction()) && age < 15)
				return false;
			if ("ENLIST".equals(c.getResultAction()) && (age < 18 || age > 45))
				return false;
		}
		return true;
	}

	// ===== 특성 효과 (C13) =====

	// 인생의 특성 효과: { 특성 ID → 효과 값 }, 없는 특성은 Lv.1(효과 0)
	public Map<String, Double> getTraitEffects(int lifeId) {
		Map<String, Double> effects = new HashMap<>();
		for (Map<String, Object> row : eDAO.selectLifeTraitEffects(lifeId)) {
			effects.put((String) row.get("TRAIT_ID"), ((Number) row.get("EFFECT_VALUE")).doubleValue());
		}
		return effects;
	}

	// 특성 하나의 효과 값 (엔진에서 STUDY 등 꺼낼 때 사용), 없으면 0
	public double getTraitEffect(int lifeId, String traitId) {
		return getTraitEffects(lifeId).getOrDefault(traitId, 0.0);
	}
}
