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

	public EventService(EventDAO eDAO) {
		this.eDAO = eDAO;
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

	private static final int AVG_MARRY_AGE = 25; // 평균 결혼 나이 (설계값, 추후 시뮬레이션 결과로 조정)

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
		case "NET_MIGRATION": // 이민: 기본 확률 × (1 - 순이민률‰), 배율 0.5~3.0배 제한 (유출 많을수록 ↑)
			Double mig = getStat(stat, "NET_MIGRATION_RATE");
			if (mig == null)
				return base; // 통계 없으면 기본 확률
			double migRate = Math.min(Math.max(1 - mig, 0.5), 3.0); // 바닥 0.5: 유입국도 최소 절반 / 천장 3.0: 극단값 방지
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

		// 1. 문장: 템플릿 문장 2~3개 중 하나를 랜덤으로 고르기
		List<String> texts = eDAO.selectTexts(t.getTemplateId());
		String sentence = texts.isEmpty() ? e.getTitle() : texts.get((int) (Math.random() * texts.size()));

		// 2. 변수 치환
		String countryName = eDAO.selectCountryName(countryCode);
		sentence = sentence.replace("{이름}", name).replace("{나이}", String.valueOf(age))
				.replace("{연도}", String.valueOf(year)).replace("{나라}", countryName == null ? countryCode : countryName)
				.replace("{변수}", e.getVarValue() == null ? "" : e.getVarValue());
		r.setLogText(sentence);

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
		r.setResultAction(t.getResultAction());

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

	// ===== 엔진 연결: 1년치 이벤트 굴리기 (EngineService가 호출) =====
	public List<EventResultDTO> rollYear(LifeDTO life) {
		int age = life.getCurrentAge();
		int year = life.getBirthYear() + age;
		List<String> expIds = eDAO.selectExpIds(life.getLifeId());
		Map<String, Double> traits = getTraitEffects(life.getLifeId()); // C13 특성 효과 (없으면 Lv.1)
		if ("Y".equals(life.getMarriedYn())) {
			expIds.add("STATE_MARRIED"); // 지금 결혼 중 (DB 저장 안 함, 조건 판정용)
		}
		List<EventResultDTO> results = new ArrayList<>();
		boolean hasChoice = false; // 한 해에 선택 이벤트는 1개만 (역사 + 인생 합쳐서)

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
				if (hasChoice)
					continue; // 이미 선택 이벤트가 있으면 건너뜀
				hasChoice = true;
			}

			results.add(buildResult(t, e, life.getName(), age, year, life.getCurrentCountry()));
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

		// 3. 투자: 50% 확률로 성공(자산 +) / 실패(자산 −)
		if ("INVEST".equals(c.getResultAction())) {
			double successRate = 0.5 * (1 + getTraitEffect(life.getLifeId(), "INVEST")); // 기본 50% × 투자 특성 (Lv.5 → 70%)
			if (Math.random() < successRate) {
				text = text + " 투자는 성공적이었다.";
			} else {
				wealth = -wealth;
				text = text + " 하지만 투자는 실패해 손해를 봤다.";
				r.setEffIncomeRate(0); // 실패하면 소득 증가 없음
				r.setEffHappiness(-Math.abs(r.getEffHappiness()) - 3); // 실패하면 행복 감소 -3은 임의의 설정값, 테스트 후 변경 가능
			}
			r.setResultAction(null); // 투자는 엔진에서 따로 처리할 상태 변화 없음
		} else {
			r.setResultAction(c.getResultAction()); // MARRY, MIGRATE 등

			// 떠난다(MIGRATE)인데 목적지가 정해지지 않았으면 → 목적지 후보 5곳
			if ("MIGRATE".equals(c.getResultAction()) && c.getMigrateTo() == null) {
				r.setNeedDestination(true);
				r.setDestinations(pickDestinations(life.getCurrentCountry(), year));
			}
		}
		r.setEffWealthRate(wealth);
		r.setMigrateTo(c.getMigrateTo());
		r.setLogText(text);

		return r;
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
		r.setLogText(year + "년, " + life.getName() + "은(는) " + d.getNameKo() + "(으)로 떠났다.");
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
			r.setChoices(eDAO.selectChoices(e.getEventId()));
		}

		// 1. 문장: 검수된 게임 문구, 없으면 제목
		String countryName = eDAO.selectCountryName(countryCode);
		String text = e.getBaseText() == null ? e.getTitle() : e.getBaseText();
		text = text.replace("{이름}", name).replace("{나이}", String.valueOf(age)).replace("{연도}", String.valueOf(year))
				.replace("{나라}", countryName == null ? countryCode : countryName);
		r.setLogText(text);

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
