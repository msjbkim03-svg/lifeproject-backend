package com.example.demo.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatDTO {
	private int msgId;
	private String countryCode;
	private int memberId;
	private String nickname;
	private String content;
	private String createdAt;
	
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String token;
	
	public int getMsgId() {
		return msgId;
	}
	public void setMsgId(int msgId) {
		this.msgId = msgId;
	}
	public String getCountryCode() {
		return countryCode;
	}
	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}
	public int getMemberId() {
		return memberId;
	}
	public void setMemberId(int memberId) {
		this.memberId = memberId;
	}
	public String getNickname() {
		return nickname;
	}
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}
	public String getToken() { return token; }
	public void setToken(String token) { this.token = token; }
}

