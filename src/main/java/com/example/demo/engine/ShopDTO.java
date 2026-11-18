package com.example.demo.engine;

import java.util.List;

// C06-3 돈 쓰기·투자 창 응답 (GET·POST /api/engine/shop, POST /api/engine/invest)
//   사고 난 뒤의 돈·능력치만 돌려줌 (나이·기록은 진행 화면이 가진 그대로 두려고 인생 전체를 돌려주지 않음)
public class ShopDTO {
	private List<ShopItemDTO> items;   // 물건 사기 + 돈 쓰기
	private List<ShopItemDTO> invests; // 여유 자금 굴리기 (done = 지금 방식)
	private String investType;         // 지금 투자 방식 (CASH, DEPOSIT, BOND, MIX, STOCK, WORLD, PROPERTY, TRADE)
	private String owns;               // 가진 물건 코드 (쉼표: CAR,TV …)
	private long assets;               // 순자산 (달러)
	private long cash;                 // 쓸 수 있는 돈 (순자산 중 0 이상)
	private double health;
	private double happiness;
	private double knowledge;
	private double relation;
	private Double krwPerUsd;          // 한국 체감 원
	private String message;            // 방금 한 일 기록 문장 (사기·하기 뒤)
	private Integer logAge;            // 그 기록의 나이

	public List<ShopItemDTO> getItems() {
		return items;
	}

	public void setItems(List<ShopItemDTO> items) {
		this.items = items;
	}

	public List<ShopItemDTO> getInvests() {
		return invests;
	}

	public void setInvests(List<ShopItemDTO> invests) {
		this.invests = invests;
	}

	public String getInvestType() {
		return investType;
	}

	public void setInvestType(String investType) {
		this.investType = investType;
	}

	public String getOwns() {
		return owns;
	}

	public void setOwns(String owns) {
		this.owns = owns;
	}

	public long getAssets() {
		return assets;
	}

	public void setAssets(long assets) {
		this.assets = assets;
	}

	public long getCash() {
		return cash;
	}

	public void setCash(long cash) {
		this.cash = cash;
	}

	public double getHealth() {
		return health;
	}

	public void setHealth(double health) {
		this.health = health;
	}

	public double getHappiness() {
		return happiness;
	}

	public void setHappiness(double happiness) {
		this.happiness = happiness;
	}

	public double getKnowledge() {
		return knowledge;
	}

	public void setKnowledge(double knowledge) {
		this.knowledge = knowledge;
	}

	public double getRelation() {
		return relation;
	}

	public void setRelation(double relation) {
		this.relation = relation;
	}

	public Double getKrwPerUsd() {
		return krwPerUsd;
	}

	public void setKrwPerUsd(Double krwPerUsd) {
		this.krwPerUsd = krwPerUsd;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Integer getLogAge() {
		return logAge;
	}

	public void setLogAge(Integer logAge) {
		this.logAge = logAge;
	}
}
