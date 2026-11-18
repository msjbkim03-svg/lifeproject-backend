package com.example.demo.quest;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestJudgeService {

	private final QuestJudgeDAO qDAO;

	public QuestJudgeService(QuestJudgeDAO qDAO) {
		this.qDAO = qDAO;
	}

	// ===== C12 포인트 지급: 달성한 퀘스트 1개당 1점 =====
	// 비회원 제외, 이미 지급한 퀘스트는 다시 안 줌, 하나라도 실패하면 전부 취소
	@Transactional
	public int givePoints(int lifeId) {
		Integer memberId = qDAO.selectLifeMember(lifeId);
		if (memberId == null)
			return 0; // 비회원

		int amount = qDAO.markPointGiven(lifeId); // 지급 표시 먼저 → 바뀐 행 수 = 줄 포인트
		if (amount == 0)
			return 0; // 달성한 퀘스트가 없거나 이미 지급함

		qDAO.addMemberPoint(memberId, amount);
		qDAO.insertPointHistory(memberId, lifeId, amount);
		return amount;
	}

	// ===== C12 퀘스트 판정: 끝난 인생의 퀘스트를 타입별로 판정해 Y/N 저장 =====
	// 반환: { 퀘스트 번호 → 달성 여부 } (포기한 인생·없는 인생은 빈 Map)
	@Transactional
	public Map<Integer, Boolean> judgeQuests(int lifeId) {
		Map<Integer, Boolean> results = new LinkedHashMap<>();
		Map<String, Object> life = qDAO.selectLifeResult(lifeId);
		if (life == null || !"ENDED".equals(life.get("STATUS")))
			return results; // 끝난 인생만 판정 (포기 ABANDONED, 진행 중 PLAYING 제외)

		for (Map<String, Object> q : qDAO.selectJudgeTargets(lifeId)) {
			int questId = num(q.get("QUEST_ID")).intValue();
			boolean ok = judgeOne((String) q.get("QUEST_TYPE"), (String) q.get("TARGET_VALUE"), life);
			qDAO.updateAchieved(lifeId, questId, ok ? "Y" : "N");
			results.put(questId, ok);
		}
		return results;
	}

	// 퀘스트 하나 판정
	private boolean judgeOne(String type, String target, Map<String, Object> life) {
		int deathAge = num(life.get("DEATH_AGE")).intValue();
		int birthYear = num(life.get("BIRTH_YEAR")).intValue();
		String eduLevel = (String) life.get("EDU_LEVEL");
		boolean graduated = "GRADUATED".equals(life.get("SCHOOL_STATUS"));

		switch (type) {
		case "AGE_EXP": { // 태어난 나라·연대 기대수명보다 N년 더 살기
			Map<String, Object> stat = qDAO.selectStatForJudge((String) life.get("BIRTH_COUNTRY"), birthYear / 10 * 10);
			double le = (stat == null || stat.get("LIFE_EXPECTANCY") == null) ? 70
					: num(stat.get("LIFE_EXPECTANCY")).doubleValue();
			return deathAge - le >= Double.parseDouble(target);
		}
		case "AGE": // N세 이상 살기
			return deathAge >= Integer.parseInt(target);
		case "EDU":
			if ("UNI".equals(target)) // 대학 졸업 (대학원 단계까지 갔으면 대학은 졸업)
				return ("UNI".equals(eduLevel) && graduated) || "GRAD".equals(eduLevel);
			return "GRAD".equals(eduLevel) && graduated; // 대학원 졸업
		case "MIGRATE": // 태어난 나라가 아닌 곳에서 인생 마치기
			return !life.get("BIRTH_COUNTRY").equals(life.get("CURRENT_COUNTRY"));
		case "CHILD": // 자녀 N명 이상
			return num(life.get("CHILD_COUNT")).intValue() >= Integer.parseInt(target);
		case "INCOME_GDP": { // 평생 평균 소득 ≥ 태어난 나라 1인당 GDP × N (인생 중간 시점 연대 기준)
			int midDecade = (birthYear + deathAge / 2) / 10 * 10;
			Map<String, Object> stat = qDAO.selectStatForJudge((String) life.get("BIRTH_COUNTRY"), midDecade);
			if (stat == null || stat.get("GDP_PER_CAPITA") == null || life.get("AVG_INCOME") == null)
				return false;
			return num(life.get("AVG_INCOME")).doubleValue() >= num(stat.get("GDP_PER_CAPITA")).doubleValue()
					* Double.parseDouble(target);
		}
		case "ASSETS": // 마지막 재산 N 이상 (빚 없이)
			return num(life.get("ASSETS")).doubleValue() >= Double.parseDouble(target);
		case "HAPPINESS": // 마지막 행복 N 이상
			return num(life.get("HAPPINESS")).doubleValue() >= Double.parseDouble(target);
		default:
			return false; // 모르는 타입은 미달성
		}
	}

	// DB 숫자(BigDecimal 등) → Number, null이면 0
	private Number num(Object o) {
		return o == null ? 0 : (Number) o;
	}

	// ===== C12 인생 종료 처리: 판정 + 포인트 지급 (엔진이 사망 처리 후 호출) =====
	// 반환: { "quests" → {퀘스트 번호 → 달성 여부}, "points" → 받은 포인트 }
	@Transactional
	public Map<String, Object> judgeAndGive(int lifeId) {
		Map<Integer, Boolean> quests = judgeQuests(lifeId); // ① 판정
		int points = givePoints(lifeId); // ② 지급 (비회원은 0)

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("quests", quests);
		result.put("points", points);
		return result;
	}

}