package com.example.demo.engine;

// 실제 플레이어 인생 한 줄 (LIFE 표에서 읽기만)
// - 메인 '방금 시작된 인생 / 방금 끝난 인생' (GET /api/engine/live)
// - 진행 화면 '같은 해에 태어난 사람들' (실제 플레이어 또래)
// 개인정보(회원 아이디·닉네임)는 넣지 않음: 게임 속 인물 이름·나라·나이만
public class LiveLifeDTO {
	private int lifeId;
	private String name;
	private String gender;
	private String countryCode;     // 태어난 나라
	private String countryName;
	private int birthYear;
	private Integer wealthClass;    // 태어난 집 형편 1~5
	private String status;          // PLAYING · ENDED
	private Integer currentAge;
	private Integer deathAge;
	private String deathCause;
	private Long agoSec;            // 몇 초 전 (서버 시각 기준: 태어남 = CREATED_AT, 끝남 = ENDED_AT)
	// 또래용: 내 나이였을 때 그 사람의 기록 (LIFE_YEAR_LOG)
	private Integer logAge;         // 그 나이 기록이 있으면 나이, 없으면 null (아직 그 나이까지 못 삶)
	private String jobName;
	private String yearCountry;     // 그 나이에 살던 나라
	private String yearCountryName;

	public int getLifeId() { return lifeId; }
	public void setLifeId(int lifeId) { this.lifeId = lifeId; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public String getGender() { return gender; }
	public void setGender(String gender) { this.gender = gender; }
	public String getCountryCode() { return countryCode; }
	public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
	public String getCountryName() { return countryName; }
	public void setCountryName(String countryName) { this.countryName = countryName; }
	public int getBirthYear() { return birthYear; }
	public void setBirthYear(int birthYear) { this.birthYear = birthYear; }
	public Integer getWealthClass() { return wealthClass; }
	public void setWealthClass(Integer wealthClass) { this.wealthClass = wealthClass; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
	public Integer getCurrentAge() { return currentAge; }
	public void setCurrentAge(Integer currentAge) { this.currentAge = currentAge; }
	public Integer getDeathAge() { return deathAge; }
	public void setDeathAge(Integer deathAge) { this.deathAge = deathAge; }
	public String getDeathCause() { return deathCause; }
	public void setDeathCause(String deathCause) { this.deathCause = deathCause; }
	public Long getAgoSec() { return agoSec; }
	public void setAgoSec(Long agoSec) { this.agoSec = agoSec; }
	public Integer getLogAge() { return logAge; }
	public void setLogAge(Integer logAge) { this.logAge = logAge; }
	public String getJobName() { return jobName; }
	public void setJobName(String jobName) { this.jobName = jobName; }
	public String getYearCountry() { return yearCountry; }
	public void setYearCountry(String yearCountry) { this.yearCountry = yearCountry; }
	public String getYearCountryName() { return yearCountryName; }
	public void setYearCountryName(String yearCountryName) { this.yearCountryName = yearCountryName; }
}
