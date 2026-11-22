package com.example.demo.member;

// 로그인 요청 JSON 을 담는 상자
// guestToken = 비회원으로 하던 인생의 임시 토큰 (없으면 null)
public class MemberLoginDTO {

	private String loginId;
	private String password;
	private String guestToken;

	public String getLoginId() { return loginId; }
	public void setLoginId(String loginId) { this.loginId = loginId; }

	public String getPassword() { return password; }
	public void setPassword(String password) { this.password = password; }

	public String getGuestToken() { return guestToken; }
	public void setGuestToken(String guestToken) { this.guestToken = guestToken; }
}
