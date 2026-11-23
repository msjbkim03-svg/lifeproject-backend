package com.example.demo.event.dto;

import java.util.List;

public class EventResultDTO {
	private String eventId;
	private String eventType; // LIFE, HIST
	private String title;
	private String logText; // 완성된 문장
	private boolean aiNarrate; // C11 AI 서술 대상

	private int effHealth;
	private int effHappiness;
	private int effKnowledge;
	private int effRelation;
	private double effIncomeRate;
	private double effWealthRate;

	private String resultAction; // 엔진이 처리할 상태 변경
	private String migrateTo;
	private double deathAdd; // 역사 사건용
	private String deathCause;

	private String expId;

	private boolean choice; // 선택 이벤트 여부
	private List<EventChoiceDTO> choices;

	private boolean needDestination; // 목적지를 골라야 하는지 (떠난다 선택 후)
	private List<DestinationDTO> destinations; // 목적지 후보
	
	private boolean ai; // AI가 쓴 문장인지 (C11, LIFE_EVENT_LOG.IS_AI)
	
	private int prisonYears; // 위험한 제안에서 붙잡혔을 때 징역 햇수 (엔진 PRISON), 나머지는 0

	public boolean isAi() {
		return ai;
	}

	public void setAi(boolean ai) {
		this.ai = ai;
	}

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

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getLogText() {
		return logText;
	}

	public void setLogText(String logText) {
		this.logText = logText;
	}

	public boolean isAiNarrate() {
		return aiNarrate;
	}

	public void setAiNarrate(boolean aiNarrate) {
		this.aiNarrate = aiNarrate;
	}

	public int getEffHealth() {
		return effHealth;
	}

	public void setEffHealth(int effHealth) {
		this.effHealth = effHealth;
	}

	public int getEffHappiness() {
		return effHappiness;
	}

	public void setEffHappiness(int effHappiness) {
		this.effHappiness = effHappiness;
	}

	public int getEffKnowledge() {
		return effKnowledge;
	}

	public void setEffKnowledge(int effKnowledge) {
		this.effKnowledge = effKnowledge;
	}

	public int getEffRelation() {
		return effRelation;
	}

	public void setEffRelation(int effRelation) {
		this.effRelation = effRelation;
	}

	public double getEffIncomeRate() {
		return effIncomeRate;
	}

	public void setEffIncomeRate(double effIncomeRate) {
		this.effIncomeRate = effIncomeRate;
	}

	public double getEffWealthRate() {
		return effWealthRate;
	}

	public void setEffWealthRate(double effWealthRate) {
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

	public double getDeathAdd() {
		return deathAdd;
	}

	public void setDeathAdd(double deathAdd) {
		this.deathAdd = deathAdd;
	}

	public String getDeathCause() {
		return deathCause;
	}

	public void setDeathCause(String deathCause) {
		this.deathCause = deathCause;
	}

	public boolean isChoice() {
		return choice;
	}

	public void setChoice(boolean choice) {
		this.choice = choice;
	}

	public List<EventChoiceDTO> getChoices() {
		return choices;
	}

	public void setChoices(List<EventChoiceDTO> choices) {
		this.choices = choices;
	}

	public String getExpId() {
		return expId;
	}

	public void setExpId(String expId) {
		this.expId = expId;
	}

	public boolean isNeedDestination() {
		return needDestination;
	}

	public void setNeedDestination(boolean needDestination) {
		this.needDestination = needDestination;
	}

	public List<DestinationDTO> getDestinations() {
		return destinations;
	}

	public void setDestinations(List<DestinationDTO> destinations) {
		this.destinations = destinations;
	}
	
	public int getPrisonYears() {
		return prisonYears;
	}

	public void setPrisonYears(int prisonYears) {
		this.prisonYears = prisonYears;
	}
}