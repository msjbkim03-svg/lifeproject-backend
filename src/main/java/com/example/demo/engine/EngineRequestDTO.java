package com.example.demo.engine;

// React에서 POST로 보내는 요청 JSON을 담는 그릇
// 예: { "decade": 1950, "guestToken": "..." } / { "lifeId": 3, "focus": "SPORT", "guestToken": "..." }
public class EngineRequestDTO {
	private int decade;        // 출생 연대 (C02)
	private int lifeId;        // 인생 번호 (C03, C04, C06)
	private String guestToken; // 비회원 임시 토큰 (회원은 Authorization 헤더 사용)
	private String focus;      // 집중 코드 (C04)
	private String jobId;      // 직업 코드 (C06)
	private int choiceId;      // 선택지 번호 (C09 선택 이벤트)
	private String countryCode; // 이민 갈 나라 (C10 목적지 고르기)
	private String itemId;     // 돈 쓰기 항목 (C06-3: BUY_CAR, DO_TRIP …)
	private String investType; // 여유 자금 굴리기 방식 (C06-3: DEPOSIT, STOCK …)
	private Integer atAge;     // 화면이 보고 있는 나이 (C06-3 기록 나이)
	private String text;       // 잠시 멈춤에 적은 한 줄 (C03, 원작 reflect)

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public int getDecade() {
		return decade;
	}

	public void setDecade(int decade) {
		this.decade = decade;
	}

	public int getLifeId() {
		return lifeId;
	}

	public void setLifeId(int lifeId) {
		this.lifeId = lifeId;
	}

	public String getGuestToken() {
		return guestToken;
	}

	public void setGuestToken(String guestToken) {
		this.guestToken = guestToken;
	}

	public String getFocus() {
		return focus;
	}

	public void setFocus(String focus) {
		this.focus = focus;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public int getChoiceId() {
		return choiceId;
	}

	public void setChoiceId(int choiceId) {
		this.choiceId = choiceId;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public String getItemId() {
		return itemId;
	}

	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	public String getInvestType() {
		return investType;
	}

	public void setInvestType(String investType) {
		this.investType = investType;
	}

	public Integer getAtAge() {
		return atAge;
	}

	public void setAtAge(Integer atAge) {
		this.atAge = atAge;
	}
}
