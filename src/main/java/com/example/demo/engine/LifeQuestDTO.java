package com.example.demo.engine;

// 진행 화면 퀘스트 칸 한 줄 (이 인생에서 고른 퀘스트)
public class LifeQuestDTO {
	private int questId;	// 퀘스트 번호
	private String questName;	// 퀘스트명
	private String description;	// 설명
	private String achieved;	// 달성 여부 (Y/N, 인생이 끝나기 전엔 null)

	public int getQuestId() {
		return questId;
	}

	public void setQuestId(int questId) {
		this.questId = questId;
	}

	public String getQuestName() {
		return questName;
	}

	public void setQuestName(String questName) {
		this.questName = questName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAchieved() {
		return achieved;
	}

	public void setAchieved(String achieved) {
		this.achieved = achieved;
	}
}
