package com.example.demo.engine;

// C09 이벤트 기록 한 줄 (LIFE_EVENT_LOG)
public class EventLogDTO {
	private int lifeId;	// 인생 번호
	private int age;	// 나이
	private int yearNo;	// 연도
	private String eventId;	// 일어난 이벤트 (GAME_EVENT)
	private Integer choiceId;	// 고른 선택지 (선택 이벤트일 때, 아니면 null)
	private String logText;	// 완성된 문장
	private String isAi = "N";	// AI 서술 여부 Y, N (C11)
	private String eventType;	// [화면용] 이벤트 종류 LIFE, HIST (GAME_EVENT, 엔진 기록은 null)
	private String alert;	// [화면용] 큰 사건 경고등 Y, N (EngineService.bigHist 와 같은 기준)

	public int getLifeId() {
		return lifeId;
	}

	public void setLifeId(int lifeId) {
		this.lifeId = lifeId;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public int getYearNo() {
		return yearNo;
	}

	public void setYearNo(int yearNo) {
		this.yearNo = yearNo;
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public Integer getChoiceId() {
		return choiceId;
	}

	public void setChoiceId(Integer choiceId) {
		this.choiceId = choiceId;
	}

	public String getLogText() {
		return logText;
	}

	public void setLogText(String logText) {
		this.logText = logText;
	}

	public String getIsAi() {
		return isAi;
	}

	public void setIsAi(String isAi) {
		this.isAi = isAi;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getAlert() {
		return alert;
	}

	public void setAlert(String alert) {
		this.alert = alert;
	}
}
