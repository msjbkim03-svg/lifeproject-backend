package com.example.demo.engine;

// C09 큰돈 드는 선택지(집·땅·가게·투자)의 비용 (화면에 "필요한 돈 · 내 돈 · 대출"로 보여 주고, 모자라면 고를 수 없음)
// EngineService.costOf 가 계산 (원작: 집값 = 중위 소득 × 5, 도시 ×1.4 / 시골 ×0.7, 대출은 집값의 80%까지)
public class ChoiceCostDTO {
	private int choiceId;     // 선택지 번호
	private String kind;      // HOUSE(집·땅), BUSINESS(가게), STOCK(주식·투자)
	private String label;     // 화면 문구 (집·땅값, 차리는 비용, 넣은 돈)
	private long cost;        // 드는 돈
	private long mine;        // 지금 가진 돈 (재산, 빚이면 0)
	private long need;        // 모자랄 때: 내 돈으로 꼭 있어야 하는 돈 (계약금 등)
	private long loan;        // 대출 받는 돈 (0이면 대출 없음)
	private long interest;    // 대출 이자 (갚는 동안 모두 합친 돈)
	private boolean ok;       // 고를 수 있는지

	public int getChoiceId() { return choiceId; }
	public void setChoiceId(int choiceId) { this.choiceId = choiceId; }
	public String getKind() { return kind; }
	public void setKind(String kind) { this.kind = kind; }
	public String getLabel() { return label; }
	public void setLabel(String label) { this.label = label; }
	public long getCost() { return cost; }
	public void setCost(long cost) { this.cost = cost; }
	public long getMine() { return mine; }
	public void setMine(long mine) { this.mine = mine; }
	public long getNeed() { return need; }
	public void setNeed(long need) { this.need = need; }
	public long getLoan() { return loan; }
	public void setLoan(long loan) { this.loan = loan; }
	public long getInterest() { return interest; }
	public void setInterest(long interest) { this.interest = interest; }
	public boolean isOk() { return ok; }
	public void setOk(boolean ok) { this.ok = ok; }
}
