package com.example.demo.engine;

import com.example.demo.event.dto.DestinationDTO;
import com.example.demo.event.dto.EventResultDTO;

import java.util.List;

public class LifeDTO {
	private int lifeId;	// 인생 번호
	private Integer memberId;	// 회원 번호 (비회원 null)
	private String guestToken;	// 비회원 임시 토큰
	private String startType;	// MEMBER, GUEST
	private String name;	// 캐릭터 이름
	private String gender;	// M, F
	private String birthCountry;	// 태어난 나라
	private String currentCountry;	// 현재 사는 나라
	private int birthYear;	// 출생 연도
	private int wealthClass;	// 가정형편 1~5
	private String isUrban;	// 도시 여부 Y, N
	private int currentAge;	// 현재 나이
	private String status;	// PLAYING, ENDED, ABANDONED
	private double health;	// 건강
	private double happiness;	// 행복
	private double knowledge;	// 지식
	private double relation;	// 관계
	private String marriedYn;	// 결혼 여부 Y, N (C09, IS_MARRIED)
	private int childCount;	// 자녀 수 (C09)
	private String parentAlive;	// 부모 생존 Y, N (C09)
	private String eduLevel;	// 학력 NONE, ELEM, MIDDLE, HIGH, UNI, GRAD (C06)
	private String schoolStatus;	// 재학 상태 ENROLLED, GRADUATED, DROPPED (C06)
	private String dropReason;	// 중퇴 사유 (C06)
	private String jobId;	// 현재·마지막 직업 (C06)
	private String jobStatus;	// EMPLOYED, UNEMPLOYED, RETIRED / 취업 전 null (C06)
	private int jobYears;	// 근속 연수 (C06)
	private int prepScore;	// 취업 준비 점수 0~2 (C06)
	private Double incomeRate;	// 현재 소득 배율 (C06)
	private long income;	// 올해 소득 (C06)
	private long assets;	// 재산, 마이너스 가능 (C06)
	private Long pension;	// 연금액 (C06)
	private String stageCode;	// 나이대 (C04)
	private String focusCode;	// 집중 선택 (C04)
	private Integer deathAge;	// 사망 나이 (C07)
	private String deathCause;	// 사망 원인 (C07)
	private Long avgIncome;	// 평균 소득 (C07 최종 결과)
	private Double lifeScore;	// 인생 점수 (C07 최종 결과)
	private Double incomeTopPct;	// 소득 상위 % (C07 최종 결과)
	private boolean needFocus;	// [화면용] 집중 선택 창 띄우기 (C04)
	private List<String> focusOptions;	// [화면용] 고를 수 있는 집중 목록 (C04)
	private boolean needJob;	// [화면용] 직업 선택 창 띄우기 (C06)
	private List<JobDTO> jobOffers;	// [화면용] 제안된 직업 3개 (C06)
	private String jobName;	// [화면용] 직업 이름
	private String countryName;	// [화면용] 나라 이름
	private String histName;	// [화면용] 당시 국가명
	private Double lifeExpectancy;	// [화면용] 기대수명
	private Double infantMortality;	// [화면용] 영아사망률
	private Double gdpPerCapita;	// [화면용] 1인당 GDP
	private List<EventResultDTO> events;	// [화면용] 올해 일어난 일 (C09)
	private String pendingEventId;	// 고르기를 기다리는 선택 이벤트 (C09, PENDING_EVENT_ID)
	private boolean needChoice;	// [화면용] 선택 이벤트 창 띄우기 (C09)
	private EventResultDTO pendingEvent;	// [화면용] 선택 이벤트 문장 + 선택지 (C09)
	private String regionCode;	// [화면용] 지금 사는 나라의 권역 (C08 마을 그림: EAST, SOUTHEAST, SOUTH, CENTRAL, WEST)
	private boolean needDestination;	// [화면용] 이민 목적지 고르기 창 띄우기 (C10, '떠난다'를 고른 뒤)
	private List<DestinationDTO> destinations;	// [화면용] 이민 목적지 후보 (윤아님 pickDestinations, 최대 5곳)

	public int getLifeId() {
		return lifeId;
	}

	public void setLifeId(int lifeId) {
		this.lifeId = lifeId;
	}

	public Integer getMemberId() {
		return memberId;
	}

	public void setMemberId(Integer memberId) {
		this.memberId = memberId;
	}

	public String getGuestToken() {
		return guestToken;
	}

	public void setGuestToken(String guestToken) {
		this.guestToken = guestToken;
	}

	public String getStartType() {
		return startType;
	}

	public void setStartType(String startType) {
		this.startType = startType;
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

	public String getCurrentCountry() {
		return currentCountry;
	}

	public void setCurrentCountry(String currentCountry) {
		this.currentCountry = currentCountry;
	}

	public int getBirthYear() {
		return birthYear;
	}

	public void setBirthYear(int birthYear) {
		this.birthYear = birthYear;
	}

	public int getWealthClass() {
		return wealthClass;
	}

	public void setWealthClass(int wealthClass) {
		this.wealthClass = wealthClass;
	}

	public String getIsUrban() {
		return isUrban;
	}

	public void setIsUrban(String isUrban) {
		this.isUrban = isUrban;
	}

	public int getCurrentAge() {
		return currentAge;
	}

	public void setCurrentAge(int currentAge) {
		this.currentAge = currentAge;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public double getHealth() {
		return health;
	}

	public void setHealth(double health) {
		this.health = health;
	}

	public double getHappiness() {
		return happiness;
	}

	public void setHappiness(double happiness) {
		this.happiness = happiness;
	}

	public double getKnowledge() {
		return knowledge;
	}

	public void setKnowledge(double knowledge) {
		this.knowledge = knowledge;
	}

	public double getRelation() {
		return relation;
	}

	public void setRelation(double relation) {
		this.relation = relation;
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

	public String getParentAlive() {
		return parentAlive;
	}

	public void setParentAlive(String parentAlive) {
		this.parentAlive = parentAlive;
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

	public String getDropReason() {
		return dropReason;
	}

	public void setDropReason(String dropReason) {
		this.dropReason = dropReason;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public String getJobStatus() {
		return jobStatus;
	}

	public void setJobStatus(String jobStatus) {
		this.jobStatus = jobStatus;
	}

	public int getJobYears() {
		return jobYears;
	}

	public void setJobYears(int jobYears) {
		this.jobYears = jobYears;
	}

	public int getPrepScore() {
		return prepScore;
	}

	public void setPrepScore(int prepScore) {
		this.prepScore = prepScore;
	}

	public Double getIncomeRate() {
		return incomeRate;
	}

	public void setIncomeRate(Double incomeRate) {
		this.incomeRate = incomeRate;
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

	public Long getPension() {
		return pension;
	}

	public void setPension(Long pension) {
		this.pension = pension;
	}

	public String getStageCode() {
		return stageCode;
	}

	public void setStageCode(String stageCode) {
		this.stageCode = stageCode;
	}

	public String getFocusCode() {
		return focusCode;
	}

	public void setFocusCode(String focusCode) {
		this.focusCode = focusCode;
	}

	public Integer getDeathAge() {
		return deathAge;
	}

	public void setDeathAge(Integer deathAge) {
		this.deathAge = deathAge;
	}

	public String getDeathCause() {
		return deathCause;
	}

	public void setDeathCause(String deathCause) {
		this.deathCause = deathCause;
	}

	public Long getAvgIncome() {
		return avgIncome;
	}

	public void setAvgIncome(Long avgIncome) {
		this.avgIncome = avgIncome;
	}

	public Double getLifeScore() {
		return lifeScore;
	}

	public void setLifeScore(Double lifeScore) {
		this.lifeScore = lifeScore;
	}

	public boolean isNeedFocus() {
		return needFocus;
	}

	public void setNeedFocus(boolean needFocus) {
		this.needFocus = needFocus;
	}

	public List<String> getFocusOptions() {
		return focusOptions;
	}

	public void setFocusOptions(List<String> focusOptions) {
		this.focusOptions = focusOptions;
	}

	public boolean isNeedJob() {
		return needJob;
	}

	public void setNeedJob(boolean needJob) {
		this.needJob = needJob;
	}

	public List<JobDTO> getJobOffers() {
		return jobOffers;
	}

	public void setJobOffers(List<JobDTO> jobOffers) {
		this.jobOffers = jobOffers;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
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

	public Double getLifeExpectancy() {
		return lifeExpectancy;
	}

	public void setLifeExpectancy(Double lifeExpectancy) {
		this.lifeExpectancy = lifeExpectancy;
	}

	public Double getInfantMortality() {
		return infantMortality;
	}

	public void setInfantMortality(Double infantMortality) {
		this.infantMortality = infantMortality;
	}

	public Double getGdpPerCapita() {
		return gdpPerCapita;
	}

	public void setGdpPerCapita(Double gdpPerCapita) {
		this.gdpPerCapita = gdpPerCapita;
	}

	public Double getIncomeTopPct() {
		return incomeTopPct;
	}

	public void setIncomeTopPct(Double incomeTopPct) {
		this.incomeTopPct = incomeTopPct;
	}


	public List<EventResultDTO> getEvents() {
		return events;
	}

	public void setEvents(List<EventResultDTO> events) {
		this.events = events;
	}

	public String getPendingEventId() {
		return pendingEventId;
	}

	public void setPendingEventId(String pendingEventId) {
		this.pendingEventId = pendingEventId;
	}

	public boolean isNeedChoice() {
		return needChoice;
	}

	public void setNeedChoice(boolean needChoice) {
		this.needChoice = needChoice;
	}

	public EventResultDTO getPendingEvent() {
		return pendingEvent;
	}

	public String getRegionCode() {
		return regionCode;
	}

	public void setRegionCode(String regionCode) {
		this.regionCode = regionCode;
	}

	public void setPendingEvent(EventResultDTO pendingEvent) {
		this.pendingEvent = pendingEvent;
	}

	public boolean isNeedDestination() {
		return needDestination;
	}

	public void setNeedDestination(boolean needDestination) {
		this.needDestination = needDestination;
	}

	public List<DestinationDTO> getDestinations() {
		return destinations;
	}

	public void setDestinations(List<DestinationDTO> destinations) {
		this.destinations = destinations;
	}
}
