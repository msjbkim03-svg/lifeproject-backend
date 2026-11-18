package com.example.demo.member;

// 아이디·비밀번호 찾기(M09) 화면에서 보낸 JSON 을 담는 상자
// sendCode · checkCode · id · resetPw 가 같이 씀 (필요한 칸만 채워서 보냄)
public class MemberFindDTO {
	private String loginId;          // 비밀번호 찾기에서만 씀
	private String email;            // 인증번호 받을 이메일
	private String purpose;          // FIND_ID 또는 RESET_PW
	private String code;             // 사용자가 입력한 인증번호 6자리
	private String newPassword;      // 새 비밀번호 (resetPw 에서만)
	private String newPasswordCheck; // 새 비밀번호 확인
	
	public MemberFindDTO() {}
	
	public MemberFindDTO(String loginId, String email, String purpose, String code, String newPassword,String newPasswordCheck) {
		this.loginId = loginId;
		this.email = email;
		this.purpose = purpose;
		this.code = code;
		this.newPassword = newPassword;
		this.newPasswordCheck = newPasswordCheck;
	}

	public String getLoginId() {
		return loginId;
	}
	public void setLoginId(String loginId) {
		this.loginId = loginId;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getPurpose() {
		return purpose;
	}
	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getNewPassword() {
		return newPassword;
	}
	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}
	public String getNewPasswordCheck() {
		return newPasswordCheck;
	}
	public void setNewPasswordCheck(String newPasswordCheck) {
		this.newPasswordCheck = newPasswordCheck;
	}
	
}
