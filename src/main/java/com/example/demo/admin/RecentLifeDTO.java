package com.example.demo.admin;

// A04 최근 끝난 인생 (게임 속 가상 인물)
public class RecentLifeDTO {
	private int lifeId;	// 인생 번호
	private String name;	// 캐릭터 이름
	private String countryCode;	// 태어난 나라 코드
	private String countryName;	// 태어난 나라 이름
	private Integer birthYear;	// 태어난 해
	private Integer deathAge;	// 수명
	private String deathCause;	// 사망 원인
	private String startType;	// MEMBER, GUEST
	private String endedAt;	// 끝난 시각 MM-DD HH24:MI (한국 시간)

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

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public String getCountryName() {
		return countryName;
	}

	public void setCountryName(String countryName) {
		this.countryName = countryName;
	}

	public Integer getBirthYear() {
		return birthYear;
	}

	public void setBirthYear(Integer birthYear) {
		this.birthYear = birthYear;
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

	public String getStartType() {
		return startType;
	}

	public void setStartType(String startType) {
		this.startType = startType;
	}

	public String getEndedAt() {
		return endedAt;
	}

	public void setEndedAt(String endedAt) {
		this.endedAt = endedAt;
	}
}
