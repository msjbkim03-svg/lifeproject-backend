package com.example.demo.engine;

// C03 진행 화면 '가족과 삶' 한 줄 (원작처럼 역할 | 이름 · 나이 · 덧붙임 · 사이 <small>직업</small>)
// 저장하지 않고 인생 번호로 늘 같게 만들어 냄 (EngineService.family)
public class FamilyMemberDTO {
	private String role;    // 어머니, 아버지, 형제자매, 배우자, 아들, 딸, 친구, 손주
	private String name;    // 이름 (형제자매·친구·손주는 비움)
	private Integer age;    // 나이 (모르면 null)
	private String job;     // 직업·한 줄 설명 (아랫줄 작은 글씨)
	private boolean alive = true;
	private String info;    // 이름·나이 뒤에 붙는 말 (예: 여성 · 기혼 · 아이 2명)
	private String bond;    // 나와의 사이 (가까운 사이 · 무난한 사이 · 서먹한 사이), 6세 전엔 비움

	public FamilyMemberDTO() {
	}

	public FamilyMemberDTO(String role, String name, Integer age, String job, boolean alive) {
		this.role = role;
		this.name = name;
		this.age = age;
		this.job = job;
		this.alive = alive;
	}

	public String getRole() { return role; }
	public void setRole(String role) { this.role = role; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public Integer getAge() { return age; }
	public void setAge(Integer age) { this.age = age; }
	public String getJob() { return job; }
	public void setJob(String job) { this.job = job; }
	public boolean isAlive() { return alive; }
	public void setAlive(boolean alive) { this.alive = alive; }
	public String getInfo() { return info; }
	public void setInfo(String info) { this.info = info; }
	public String getBond() { return bond; }
	public void setBond(String bond) { this.bond = bond; }

	// 덧붙임·사이를 이어서 넣기 (family() 에서 한 줄로 쓰려고)
	public FamilyMemberDTO with(String info, String bond) {
		this.info = info;
		this.bond = bond;
		return this;
	}
}
