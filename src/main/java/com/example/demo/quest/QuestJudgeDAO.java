package com.example.demo.quest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class QuestJudgeDAO {

	private final SqlSessionTemplate mybatis;

	public QuestJudgeDAO(SqlSessionTemplate mybatis) {
		this.mybatis = mybatis;
	}

	// 인생의 회원 번호 (비회원이면 null)
	public Integer selectLifeMember(int lifeId) {
		return mybatis.selectOne("QuestJudge.selectLifeMember", lifeId);
	}

	// 지급 표시하고 바뀐 행 수 반환
	public int markPointGiven(int lifeId) {
		return mybatis.update("QuestJudge.markPointGiven", lifeId);
	}

	// 회원 포인트 증가
	public void addMemberPoint(int memberId, int amount) {
		Map<String, Object> param = new HashMap<>();
		param.put("memberId", memberId);
		param.put("amount", amount);
		mybatis.update("QuestJudge.addMemberPoint", param);
	}

	// 포인트 내역 저장
	public void insertPointHistory(int memberId, int lifeId, int amount) {
		Map<String, Object> param = new HashMap<>();
		param.put("memberId", memberId);
		param.put("lifeId", lifeId);
		param.put("amount", amount);
		mybatis.insert("QuestJudge.insertPointHistory", param);
	}
	
	// 판정할 퀘스트 (QUEST_ID, QUEST_TYPE, TARGET_VALUE)
	public List<Map<String, Object>> selectJudgeTargets(int lifeId) {
		return mybatis.selectList("QuestJudge.selectJudgeTargets", lifeId);
	}

	// 판정에 쓸 인생 결과
	public Map<String, Object> selectLifeResult(int lifeId) {
		return mybatis.selectOne("QuestJudge.selectLifeResult", lifeId);
	}

	// 나라·연대 통계 (기대수명, 1인당 GDP)
	public Map<String, Object> selectStatForJudge(String countryCode, int decade) {
		Map<String, Object> param = new HashMap<>();
		param.put("countryCode", countryCode);
		param.put("decade", decade);
		return mybatis.selectOne("QuestJudge.selectStatForJudge", param);
	}

	// 판정 결과 저장 (Y/N)
	public void updateAchieved(int lifeId, int questId, String achieved) {
		Map<String, Object> param = new HashMap<>();
		param.put("lifeId", lifeId);
		param.put("questId", questId);
		param.put("achieved", achieved);
		mybatis.update("QuestJudge.updateAchieved", param);
	}
	
}