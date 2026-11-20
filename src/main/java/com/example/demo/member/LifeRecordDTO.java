package com.example.demo.member;

// 전생 기록(M07) 표 한 줄
public class LifeRecordDTO {

	private int lifeNo;          // 몇 번째 인생 (가장 오래된 인생이 1)
	private int lifeId;          // 인생 번호 (결과 보기 /result/lifeId)
	private String birthCountryName;  // 태어난 나라
	private int birthYear;       // 출생 연도
	private Integer deathAge;    // 수명 (int 는 null 을 못 담아서 Integer)
	private String deathCause;   // 사망 원인
	private String endedAt;      // 끝난 날짜 (YYYY-MM-DD)
	private int questTotal;      // 고른 퀘스트 수
	private int questDone;       // 달성한 퀘스트 수
	private int earnedPoint;           // 이 인생으로 받은 포인트

	public int getLifeNo() { return lifeNo; }
	public void setLifeNo(int lifeNo) { this.lifeNo = lifeNo; }

	public int getLifeId() { return lifeId; }
	public void setLifeId(int lifeId) { this.lifeId = lifeId; }

	public String getBirthCountryName() { return birthCountryName; }
	public void setBirthCountryName(String birthCountryName) { this.birthCountryName = birthCountryName; }

	public int getBirthYear() { return birthYear; }
	public void setBirthYear(int birthYear) { this.birthYear = birthYear; }

	public Integer getDeathAge() { return deathAge; }
	public void setDeathAge(Integer deathAge) { this.deathAge = deathAge; }

	public String getDeathCause() { return deathCause; }
	public void setDeathCause(String deathCause) { this.deathCause = deathCause; }

	public String getEndedAt() { return endedAt; }
	public void setEndedAt(String endedAt) { this.endedAt = endedAt; }

	public int getQuestTotal() { return questTotal; }
	public void setQuestTotal(int questTotal) { this.questTotal = questTotal; }

	public int getQuestDone() { return questDone; }
	public void setQuestDone(int questDone) { this.questDone = questDone; }

	public int getEarnedPoint() { return earnedPoint; }
	public void setEarnedPoint(int earnedPoint) { this.earnedPoint = earnedPoint; }
}
