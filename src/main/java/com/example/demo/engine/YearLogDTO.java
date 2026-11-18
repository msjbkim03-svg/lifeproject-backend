package com.example.demo.engine;

// C03 인생 지도 한 칸 = 한 해 기록 (LIFE_YEAR_LOG)
public class YearLogDTO {
	private int age;	// 나이
	private int yearNo;	// 연도
	private String countryCode;	// 그해 사는 나라
	private int health;	// 건강
	private int happiness;	// 행복
	private int knowledge;	// 지식
	private int relation;	// 관계
	private String focusCode;	// 그해 집중
	private String jobId;	// 직업
	private long income;	// 소득
	private long assets;	// 재산 (순자산)

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public int getYearNo() {
		return yearNo;
	}

	public void setYearNo(int yearNo) {
		this.yearNo = yearNo;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public int getHealth() {
		return health;
	}

	public void setHealth(int health) {
		this.health = health;
	}

	public int getHappiness() {
		return happiness;
	}

	public void setHappiness(int happiness) {
		this.happiness = happiness;
	}

	public int getKnowledge() {
		return knowledge;
	}

	public void setKnowledge(int knowledge) {
		this.knowledge = knowledge;
	}

	public int getRelation() {
		return relation;
	}

	public void setRelation(int relation) {
		this.relation = relation;
	}

	public String getFocusCode() {
		return focusCode;
	}

	public void setFocusCode(String focusCode) {
		this.focusCode = focusCode;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public long getIncome() {
		return income;
	}

	public void setIncome(long income) {
		this.income = income;
	}

	public long getAssets() {
		return assets;
	}

	public void setAssets(long assets) {
		this.assets = assets;
	}
}
