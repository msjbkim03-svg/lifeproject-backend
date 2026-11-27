package com.example.demo.member;

// 개인정보 수정(M05) · 회원 탈퇴(M06) 화면에서 보낸 JSON 을 담는 상자
// 탈퇴할 때는 currentPassword 만 채워서 보냄
public class MemberUpdateDTO {

	private int memberId;            // 회원 번호 (화면에서 안 보냄, 서버가 토큰에서 꺼내 넣음)
	private String nickname;         // 바꿀 닉네임
	private String email;            // 바꿀 이메일
	private String phone;            // 바꿀 전화번호 (010-1234-5678 도 OK, 서버에서 숫자만 남김)
	private String currentPassword;  // 현재 비밀번호 (본인 확인용)
	private String newPassword;      // 새 비밀번호 (비워 두면 안 바뀜)
	private String newPasswordCheck; // 새 비밀번호 확인
	private String emailCode;        // 이메일을 바꿀 때 새 이메일로 받은 인증번호 6자리
	private String googleCredential; // 구글 소셜 계정 탈퇴 시 재인증용 Google 토큰

	public int getMemberId() { return memberId; }
	public void setMemberId(int memberId) { this.memberId = memberId; }

	public String getNickname() { return nickname; }
	public void setNickname(String nickname) { this.nickname = nickname; }

	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	public String getPhone() { return phone; }
	public void setPhone(String phone) { this.phone = phone; }

	public String getCurrentPassword() { return currentPassword; }
	public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }

	public String getNewPassword() { return newPassword; }
	public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

	public String getNewPasswordCheck() { return newPasswordCheck; }
	public void setNewPasswordCheck(String newPasswordCheck) { this.newPasswordCheck = newPasswordCheck; }

	public String getEmailCode() { return emailCode; }
	public void setEmailCode(String emailCode) { this.emailCode = emailCode; }

	public String getGoogleCredential() { return googleCredential; }
	public void setGoogleCredential(String googleCredential) { this.googleCredential = googleCredential; }
}
