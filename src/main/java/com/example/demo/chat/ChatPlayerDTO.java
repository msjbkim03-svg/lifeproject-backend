package com.example.demo.chat;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatPlayerDTO {

	private int memberId;
	private String nickname;
	private int age;
	private String gender;
	private String place;
	private double x;
	private String dir;
	private boolean walking;
	private Map<String, String> costume;

	@JsonIgnore
	private String countryCode;

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String token;

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String guestToken;

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String clientId;

	public int getMemberId() { return memberId; }
	public void setMemberId(int memberId) { this.memberId = memberId; }

	public String getNickname() { return nickname; }
	public void setNickname(String nickname) { this.nickname = nickname; }

	public int getAge() { return age; }
	public void setAge(int age) { this.age = age; }

	public String getGender() { return gender; }
	public void setGender(String gender) { this.gender = gender; }

	public String getPlace() { return place; }
	public void setPlace(String place) { this.place = place; }

	public double getX() { return x; }
	public void setX(double x) { this.x = x; }

	public String getDir() { return dir; }
	public void setDir(String dir) { this.dir = dir; }

	public boolean isWalking() { return walking; }
	public void setWalking(boolean walking) { this.walking = walking; }

	public Map<String, String> getCostume() { return costume; }
	public void setCostume(Map<String, String> costume) { this.costume = costume; }

	public String getCountryCode() { return countryCode; }
	public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

	public String getToken() { return token; }
	public void setToken(String token) { this.token = token; }

	public String getGuestToken() { return guestToken; }
	public void setGuestToken(String guestToken) { this.guestToken = guestToken; }

	public String getClientId() { return clientId; }
	public void setClientId(String clientId) { this.clientId = clientId; }
}
