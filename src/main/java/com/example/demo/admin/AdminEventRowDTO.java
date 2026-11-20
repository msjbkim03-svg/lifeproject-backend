package com.example.demo.admin;

//A03 관리자 이벤트 목록 한 줄 (GAME_EVENT + 템플릿 이름 + 역사 사건 정보 + 선택지 수)
public class AdminEventRowDTO {
	private String eventId;
	private String eventType; // LIFE / HIST
	private String templateId;
	private String templateName; // 인생 이벤트만
	private String histEventId;
	private String histTitle; // 역사 이벤트만 (HISTORY_EVENT.TITLE)
	private String countryCode; // 역사 이벤트만
	private Integer startYear; // 역사 이벤트만
	private Integer endYear; // 역사 이벤트만
	private String title;
	private String baseText;
	private String isChoice; // Y/N
	private String aiNarrate; // Y/N
	private String isReviewed; // Y/N
	private Double probability;
	private Integer choiceCount; // 선택지 수

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getTemplateId() {
		return templateId;
	}

	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}

	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	public String getHistEventId() {
		return histEventId;
	}

	public void setHistEventId(String histEventId) {
		this.histEventId = histEventId;
	}

	public String getHistTitle() {
		return histTitle;
	}

	public void setHistTitle(String histTitle) {
		this.histTitle = histTitle;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public Integer getStartYear() {
		return startYear;
	}

	public void setStartYear(Integer startYear) {
		this.startYear = startYear;
	}

	public Integer getEndYear() {
		return endYear;
	}

	public void setEndYear(Integer endYear) {
		this.endYear = endYear;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getBaseText() {
		return baseText;
	}

	public void setBaseText(String baseText) {
		this.baseText = baseText;
	}

	public String getIsChoice() {
		return isChoice;
	}

	public void setIsChoice(String isChoice) {
		this.isChoice = isChoice;
	}

	public String getAiNarrate() {
		return aiNarrate;
	}

	public void setAiNarrate(String aiNarrate) {
		this.aiNarrate = aiNarrate;
	}

	public String getIsReviewed() {
		return isReviewed;
	}

	public void setIsReviewed(String isReviewed) {
		this.isReviewed = isReviewed;
	}

	public Double getProbability() {
		return probability;
	}

	public void setProbability(Double probability) {
		this.probability = probability;
	}

	public Integer getChoiceCount() {
		return choiceCount;
	}

	public void setChoiceCount(Integer choiceCount) {
		this.choiceCount = choiceCount;
	}
}