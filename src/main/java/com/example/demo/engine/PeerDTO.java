package com.example.demo.engine;

// C03 진행 화면 '같은 해에 태어난 사람들' (원작 makeCompanions)
// 11/25 동현님: 실제 플레이어 인생을 먼저 (같은 연대, 같은 해에 가까운 순) → 4명이 안 되면 아래처럼 만든 또래로 채움
// 같은 해에 등록된 나라에서 태어난 4명을 인생 번호로 늘 같게 만들고, 내 나이일 때 그 사람의 처지를 보여 줌
public class PeerDTO {
	private String name;
	private String countryCode;
	private String countryName;
	private String gender;
	private String status;   // 내 나이일 때: 어린 시절, 학교 다니는 중, 일하는 중, 61세에 세상을 떠남 …
	private boolean alive = true;
	private boolean real;    // 실제 플레이어의 인생이면 true (모자라면 예전처럼 만든 또래로 채움)
	private Integer age;     // 화면에 붙일 나이 (실제 플레이어가 아직 내 나이까지 못 살았으면 그 사람 지금 나이)
	private Integer birthYear;

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public String getCountryCode() { return countryCode; }
	public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
	public String getCountryName() { return countryName; }
	public void setCountryName(String countryName) { this.countryName = countryName; }
	public String getGender() { return gender; }
	public void setGender(String gender) { this.gender = gender; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
	public boolean isAlive() { return alive; }
	public void setAlive(boolean alive) { this.alive = alive; }
	public boolean isReal() { return real; }
	public void setReal(boolean real) { this.real = real; }
	public Integer getAge() { return age; }
	public void setAge(Integer age) { this.age = age; }
	public Integer getBirthYear() { return birthYear; }
	public void setBirthYear(Integer birthYear) { this.birthYear = birthYear; }
}
