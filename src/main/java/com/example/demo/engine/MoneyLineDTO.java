package com.example.demo.engine;

// C06 '지난 1년 순자산' 한 줄 (원작 살림 패널처럼: 번 돈·남긴 돈·양육비·경조사비·빚 이자 …)
// amount 는 달러, 화면에서 krwPerUsd 를 곱해 '한국 체감' 원으로 보여 줌
public class MoneyLineDTO {
	private String label;
	private long amount;

	public MoneyLineDTO() {
	}

	public MoneyLineDTO(String label, long amount) {
		this.label = label;
		this.amount = amount;
	}

	public String getLabel() { return label; }
	public void setLabel(String label) { this.label = label; }
	public long getAmount() { return amount; }
	public void setAmount(long amount) { this.amount = amount; }
}
