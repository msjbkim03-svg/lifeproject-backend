package com.example.demo.quest;

import java.util.List;

// 고른 퀘스트 저장 요청 (C01)
public class QuestSaveDTO {

	private int lifeId;            // 방금 태어난 인생 번호
	private List<Integer> questIds; // 고른 퀘스트 번호 (1~3개)

	public int getLifeId() { return lifeId; }
	public void setLifeId(int lifeId) { this.lifeId = lifeId; }

	public List<Integer> getQuestIds() { return questIds; }
	public void setQuestIds(List<Integer> questIds) { this.questIds = questIds; }
}
