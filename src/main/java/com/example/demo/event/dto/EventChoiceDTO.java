package com.example.demo.event.dto;

public class EventChoiceDTO {

	private Integer choiceId; // 선택지 번호
	private String eventId; // 이벤트 ID
	private Integer choiceOrder; // 순서 (1, 2)
	private String choiceText; // 선택지 문구 (버튼)
	private String resultText; // 고른 뒤 결과 문구
	private Integer effHealth; // 건강 효과
	private Integer effHappiness; // 행복 효과
	private Integer effKnowledge; // 지식 효과
	private Integer effRelation; // 관계 효과
	private Double effIncomeRate; // 소득 변화율
	private Double effWealthRate; // 자산 변화율
	private String resultAction; // MARRY, MIGRATE, INVEST 등
	private String migrateTo; // 이민 목적지
	private Double successRate; // 성공 확률 (투자·치료·참여·상담), 성공·실패 없는 선택지는 null

	public Integer getChoiceId() {
		return choiceId;
	}

	public void setChoiceId(Integer choiceId) {
		this.choiceId = choiceId;
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public Integer getChoiceOrder() {
		return choiceOrder;
	}

	public void setChoiceOrder(Integer choiceOrder) {
		this.choiceOrder = choiceOrder;
	}

	public String getChoiceText() {
		return choiceText;
	}

	public void setChoiceText(String choiceText) {
		this.choiceText = choiceText;
	}

	public String getResultText() {
		return resultText;
	}

	public void setResultText(String resultText) {
		this.resultText = resultText;
	}

	public Integer getEffHealth() {
		return effHealth;
	}

	public void setEffHealth(Integer effHealth) {
		this.effHealth = effHealth;
	}

	public Integer getEffHappiness() {
		return effHappiness;
	}

	public void setEffHappiness(Integer effHappiness) {
		this.effHappiness = effHappiness;
	}

	public Integer getEffKnowledge() {
		return effKnowledge;
	}

	public void setEffKnowledge(Integer effKnowledge) {
		this.effKnowledge = effKnowledge;
	}

	public Integer getEffRelation() {
		return effRelation;
	}

	public void setEffRelation(Integer effRelation) {
		this.effRelation = effRelation;
	}

	public Double getEffIncomeRate() {
		return effIncomeRate;
	}

	public void setEffIncomeRate(Double effIncomeRate) {
		this.effIncomeRate = effIncomeRate;
	}

	public Double getEffWealthRate() {
		return effWealthRate;
	}

	public void setEffWealthRate(Double effWealthRate) {
		this.effWealthRate = effWealthRate;
	}

	public String getResultAction() {
		return resultAction;
	}

	public void setResultAction(String resultAction) {
		this.resultAction = resultAction;
	}

	public String getMigrateTo() {
		return migrateTo;
	}

	public void setMigrateTo(String migrateTo) {
		this.migrateTo = migrateTo;
	}

	public Double getSuccessRate() {
		return successRate;
	}

	public void setSuccessRate(Double successRate) {
		this.successRate = successRate;
	}
}