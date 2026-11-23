package com.example.demo.result;

import java.util.List;

// C18 인생 결과 화면에 보낼 값 (요약 + 그래프 + 인생 기록 + 퀘스트)
public class ResultDTO {
	private int lifeId;	// 인생 번호
	private String name;	// 캐릭터 이름
	private String gender;	// 성별 M, F
	private String birthCountry;	// 태어난 나라 코드
	private String countryName;	// 태어난 나라 이름
	private String histName;	// 태어날 때 당시 국가명 (C16 COUNTRY_NAME_HIST)
	private String currentCountry;	// 마지막에 살던 나라 코드 (이민·피란 시 다름)
	private String currentCountryName;	// 마지막에 살던 나라 이름
	private int birthYear;	// 출생 연도
	private Integer deathAge;	// 수명
	private Integer deathYear;	// 사망 연도
	private String deathCause;	// 사망 원인 (C07)
	private Double lifeExpectancy;	// 태어난 나라·연대의 실제 기대수명 (비교용, C16)
	private String eduLevel;	// 최종 학력
	private String schoolStatus;	// 재학 상태 GRADUATED, DROPPED 등
	private String jobId;	// 마지막 직업 코드
	private String jobName;	// 마지막 직업 이름
	private String jobStatus;	// 직업 상태
	private Long avgIncome;	// 평균 소득 (C07)
	private Double incomeTopPct;	// 소득 상위 % (C07)
	private Double lifeScore;	// 인생 점수 (C07)
	private int health;	// 마지막 건강 (반올림)
	private int happiness;	// 마지막 행복
	private int knowledge;	// 마지막 지식
	private int relation;	// 마지막 관계
	private String marriedYn;	// 결혼 여부 Y, N
	private Integer weddingCount;	// 결혼한 횟수 (인생 기록 문장으로 셈)
	private Integer divorceCount;	// 이혼 횟수
	private Integer widowCount;	// 사별 횟수
	private int childCount;	// 자녀 수
	private String startType;	// MEMBER, GUEST
	private String status;	// PLAYING, ENDED, ABANDONED
	private Integer memorialPostId;	// 추모관에 올린 글 번호 (안 올렸으면 null, B01)
	private int pointEarned;	// 이 인생으로 받은 포인트 (C12)
	private List<YearLogDTO> years;	// 나이별 기록 (능력치 그래프)
	private List<EventLogDTO> events;	// 인생 기록 (사건)
	private List<QuestResultDTO> quests;	// 퀘스트 달성 여부

	public int getLifeId() {
		return lifeId;
	}

	public void setLifeId(int lifeId) {
		this.lifeId = lifeId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getBirthCountry() {
		return birthCountry;
	}

	public void setBirthCountry(String birthCountry) {
		this.birthCountry = birthCountry;
	}

	public String getCountryName() {
		return countryName;
	}

	public void setCountryName(String countryName) {
		this.countryName = countryName;
	}

	public String getHistName() {
		return histName;
	}

	public void setHistName(String histName) {
		this.histName = histName;
	}

	public String getCurrentCountry() {
		return currentCountry;
	}

	public void setCurrentCountry(String currentCountry) {
		this.currentCountry = currentCountry;
	}

	public String getCurrentCountryName() {
		return currentCountryName;
	}

	public void setCurrentCountryName(String currentCountryName) {
		this.currentCountryName = currentCountryName;
	}

	public int getBirthYear() {
		return birthYear;
	}

	public void setBirthYear(int birthYear) {
		this.birthYear = birthYear;
	}

	public Integer getDeathAge() {
		return deathAge;
	}

	public void setDeathAge(Integer deathAge) {
		this.deathAge = deathAge;
	}

	public Integer getDeathYear() {
		return deathYear;
	}

	public void setDeathYear(Integer deathYear) {
		this.deathYear = deathYear;
	}

	public String getDeathCause() {
		return deathCause;
	}

	public void setDeathCause(String deathCause) {
		this.deathCause = deathCause;
	}

	public Double getLifeExpectancy() {
		return lifeExpectancy;
	}

	public void setLifeExpectancy(Double lifeExpectancy) {
		this.lifeExpectancy = lifeExpectancy;
	}

	public String getEduLevel() {
		return eduLevel;
	}

	public void setEduLevel(String eduLevel) {
		this.eduLevel = eduLevel;
	}

	public String getSchoolStatus() {
		return schoolStatus;
	}

	public void setSchoolStatus(String schoolStatus) {
		this.schoolStatus = schoolStatus;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public String getJobStatus() {
		return jobStatus;
	}

	public void setJobStatus(String jobStatus) {
		this.jobStatus = jobStatus;
	}

	public Long getAvgIncome() {
		return avgIncome;
	}

	public void setAvgIncome(Long avgIncome) {
		this.avgIncome = avgIncome;
	}

	public Double getIncomeTopPct() {
		return incomeTopPct;
	}

	public void setIncomeTopPct(Double incomeTopPct) {
		this.incomeTopPct = incomeTopPct;
	}

	public Double getLifeScore() {
		return lifeScore;
	}

	public void setLifeScore(Double lifeScore) {
		this.lifeScore = lifeScore;
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

	public Integer getWeddingCount() {
		return weddingCount;
	}

	public void setWeddingCount(Integer weddingCount) {
		this.weddingCount = weddingCount;
	}

	public Integer getDivorceCount() {
		return divorceCount;
	}

	public void setDivorceCount(Integer divorceCount) {
		this.divorceCount = divorceCount;
	}

	public Integer getWidowCount() {
		return widowCount;
	}

	public void setWidowCount(Integer widowCount) {
		this.widowCount = widowCount;
	}

	public String getMarriedYn() {
		return marriedYn;
	}

	public void setMarriedYn(String marriedYn) {
		this.marriedYn = marriedYn;
	}

	public int getChildCount() {
		return childCount;
	}

	public void setChildCount(int childCount) {
		this.childCount = childCount;
	}

	public String getStartType() {
		return startType;
	}

	public void setStartType(String startType) {
		this.startType = startType;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Integer getMemorialPostId() {
		return memorialPostId;
	}

	public void setMemorialPostId(Integer memorialPostId) {
		this.memorialPostId = memorialPostId;
	}

	public int getPointEarned() {
		return pointEarned;
	}

	public void setPointEarned(int pointEarned) {
		this.pointEarned = pointEarned;
	}

	public List<YearLogDTO> getYears() {
		return years;
	}

	public void setYears(List<YearLogDTO> years) {
		this.years = years;
	}

	public List<EventLogDTO> getEvents() {
		return events;
	}

	public void setEvents(List<EventLogDTO> events) {
		this.events = events;
	}

	public List<QuestResultDTO> getQuests() {
		return quests;
	}

	public void setQuests(List<QuestResultDTO> quests) {
		this.quests = quests;
	}
}
