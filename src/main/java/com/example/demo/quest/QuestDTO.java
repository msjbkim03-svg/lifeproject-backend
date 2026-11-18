package com.example.demo.quest;

// QUEST 테이블 한 줄 (C01 퀘스트 목록)
public class QuestDTO {

	private int questId;        // 퀘스트 번호
	private String questName;   // 퀘스트명 (예: 100세 장수하기)
	private String questType;   // 판정 종류 (AGE, EDU, MIGRATE …) - C12 판정에 사용
	private String targetValue; // 고정 목표값 (예: 100, UNI)
	private String description; // 설명

	public int getQuestId() { return questId; }
	public void setQuestId(int questId) { this.questId = questId; }

	public String getQuestName() { return questName; }
	public void setQuestName(String questName) { this.questName = questName; }

	public String getQuestType() { return questType; }
	public void setQuestType(String questType) { this.questType = questType; }

	public String getTargetValue() { return targetValue; }
	public void setTargetValue(String targetValue) { this.targetValue = targetValue; }

	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
}
