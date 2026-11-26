package com.example.demo.admin;

// A02 관리자 채팅 관리 - 마을 채팅 1줄
public class AdminChatDTO {

	private int msgId;
	private String countryCode;
	private String countryName;
	private int memberId;
	private String loginId;
	private String nickname;
	private String content;
	private String isDeleted;
	private String createdAt;

	public int getMsgId() { return msgId; }
	public void setMsgId(int msgId) { this.msgId = msgId; }

	public String getCountryCode() { return countryCode; }
	public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

	public String getCountryName() { return countryName; }
	public void setCountryName(String countryName) { this.countryName = countryName; }

	public int getMemberId() { return memberId; }
	public void setMemberId(int memberId) { this.memberId = memberId; }

	public String getLoginId() { return loginId; }
	public void setLoginId(String loginId) { this.loginId = loginId; }

	public String getNickname() { return nickname; }
	public void setNickname(String nickname) { this.nickname = nickname; }

	public String getContent() { return content; }
	public void setContent(String content) { this.content = content; }

	public String getIsDeleted() { return isDeleted; }
	public void setIsDeleted(String isDeleted) { this.isDeleted = isDeleted; }

	public String getCreatedAt() { return createdAt; }
	public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
