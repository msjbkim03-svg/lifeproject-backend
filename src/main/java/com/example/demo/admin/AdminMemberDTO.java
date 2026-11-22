package com.example.demo.admin;

// A01 관리자 회원 관리 - 회원 목록 한 줄
public class AdminMemberDTO {

	private int memberId;
	private String loginId;
	private String nickname;
	private String email;
	private String phone;        // 숫자만 (화면에서 하이픈 붙임)
	private String memberRole;   // USER / ADMIN
	private int point;
	private String status;       // ACTIVE / WITHDRAWN
	private String joinDate;     // SQL 에서 'YYYY-MM-DD' 글자로 바꿔서 받음
	private int lifeCount;       // 끝난 인생 수

	public int getMemberId() {
		return memberId;
	}

	public void setMemberId(int memberId) {
		this.memberId = memberId;
	}

	public String getLoginId() {
		return loginId;
	}

	public void setLoginId(String loginId) {
		this.loginId = loginId;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getMemberRole() {
		return memberRole;
	}

	public void setMemberRole(String memberRole) {
		this.memberRole = memberRole;
	}

	public int getPoint() {
		return point;
	}

	public void setPoint(int point) {
		this.point = point;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getJoinDate() {
		return joinDate;
	}

	public void setJoinDate(String joinDate) {
		this.joinDate = joinDate;
	}

	public int getLifeCount() {
		return lifeCount;
	}

	public void setLifeCount(int lifeCount) {
		this.lifeCount = lifeCount;
	}
}
