package com.example.demo.admin;

// A04 최근 14일 하루 플레이 수 (한국 날짜 기준, DB 시각은 UTC라 +9시간)
public class DailyPlayDTO {
	private String day;	// 날짜 YYYY-MM-DD
	private int memberStarts;	// 그날 시작된 회원 인생
	private int guestStarts;	// 그날 시작된 비회원 인생 (지워진 비회원 포함)
	private int ended;	// 그날 끝까지 산 인생
	private int abandoned;	// 그날 중간에 나간 인생

	public String getDay() {
		return day;
	}

	public void setDay(String day) {
		this.day = day;
	}

	public int getMemberStarts() {
		return memberStarts;
	}

	public void setMemberStarts(int memberStarts) {
		this.memberStarts = memberStarts;
	}

	public int getGuestStarts() {
		return guestStarts;
	}

	public void setGuestStarts(int guestStarts) {
		this.guestStarts = guestStarts;
	}

	public int getEnded() {
		return ended;
	}

	public void setEnded(int ended) {
		this.ended = ended;
	}

	public int getAbandoned() {
		return abandoned;
	}

	public void setAbandoned(int abandoned) {
		this.abandoned = abandoned;
	}
}
