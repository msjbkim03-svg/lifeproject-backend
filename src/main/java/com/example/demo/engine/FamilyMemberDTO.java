package com.example.demo.engine;

// C03 진행 화면 '가족과 삶' 한 줄 (원작처럼 어머니·아버지 이름·나이·직업, 형제자매, 배우자, 자녀)
// 저장하지 않고 인생 번호로 늘 같게 만들어 냄 (EngineService.family)
public class FamilyMemberDTO {
	private String role;    // 어머니, 아버지, 형제자매, 배우자, 자녀
	private String name;    // 이름 (형제자매·자녀는 비움)
	private Integer age;    // 나이 (모르면 null)
	private String job;     // 직업·한 줄 설명
	private boolean alive = true;

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
}
