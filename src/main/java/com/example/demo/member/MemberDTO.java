package com.example.demo.member;

import java.util.Date;

// MEMBER 테이블 한 줄을 담는 상자
// 회원가입 화면에서 보낸 JSON도 이 상자에 담겨서 들어온다
public class MemberDTO {

	private int memberId;        // MEMBER_ID  (SEQ_MEMBER로 자동 번호)
	private String loginId;      // LOGIN_ID   아이디
	private String password;     // PASSWORD   비밀번호 (DB에는 BCrypt 값)
	private String passwordCheck;// 비밀번호 확인 (DB에는 없음, 화면 검사용)
	private String nickname;     // NICKNAME   닉네임
	private String email;        // EMAIL      이메일
	private String memberRole;   // MEMBER_ROLE USER / ADMIN
	private int point;
	private String status;       // STATUS     ACTIVE / WITHDRAWN
	private Date joinDate;
	private String phone;        // PHONE      전화번호 (숫자만 저장)
	private String privacyAgreed;// PRIVACY_AGREED 개인정보 활용 동의 Y / N
	private String captchaToken; // 로봇 확인 토큰 (DB에는 없음, 회원가입 때 구글 확인용)

	public int getMemberId() { return memberId; }
	public void setMemberId(int memberId) { this.memberId = memberId; }

	public String getLoginId() { return loginId; }
	public void setLoginId(String loginId) { this.loginId = loginId; }

	public String getPassword() { return password; }
	public void setPassword(String password) { this.password = password; }

	public String getPasswordCheck() { return passwordCheck; }
	public void setPasswordCheck(String passwordCheck) { this.passwordCheck = passwordCheck; }

	public String getNickname() { return nickname; }
	public void setNickname(String nickname) { this.nickname = nickname; }

	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	public String getMemberRole() { return memberRole; }
	public void setMemberRole(String memberRole) { this.memberRole = memberRole; }

	public int getPoint() { return point; }
	public void setPoint(int point) { this.point = point; }

	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }

	public Date getJoinDate() { return joinDate; }
	public void setJoinDate(Date joinDate) { this.joinDate = joinDate; }

	public String getPhone() { return phone; }
	public void setPhone(String phone) { this.phone = phone; }

	public String getPrivacyAgreed() { return privacyAgreed; }
	public void setPrivacyAgreed(String privacyAgreed) { this.privacyAgreed = privacyAgreed; }

	public String getCaptchaToken() { return captchaToken; }
	public void setCaptchaToken(String captchaToken) { this.captchaToken = captchaToken; }
}
