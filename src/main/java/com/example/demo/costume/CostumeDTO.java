package com.example.demo.costume;

import com.fasterxml.jackson.annotation.JsonIgnore;

// 모자·머리·옷 하나 (값 price 는 Service 가 계산해서 넣음, 달러)
//   part: HAT / HAIR / CLOTHES
//   look: 동현님 costumeData.js 상품 코드 (HAIR_01, TOP_01, HAT_01 …)
public class CostumeDTO {
	private int costumeId;
	private String costumeName;
	private String part;
	private double priceRate;
	private String look;
	private String owned;
	private String equipped;
	private long price;

	public int getCostumeId() { return costumeId; }
	public void setCostumeId(int costumeId) { this.costumeId = costumeId; }
	public String getCostumeName() { return costumeName; }
	public void setCostumeName(String costumeName) { this.costumeName = costumeName; }
	public String getPart() { return part; }
	public void setPart(String part) { this.part = part; }
	@JsonIgnore
	public double getPriceRate() { return priceRate; }
	public void setPriceRate(double priceRate) { this.priceRate = priceRate; }
	public String getLook() { return look; }
	public void setLook(String look) { this.look = look; }
	public String getOwned() { return owned; }
	public void setOwned(String owned) { this.owned = owned; }
	public String getEquipped() { return equipped; }
	public void setEquipped(String equipped) { this.equipped = equipped; }
	public long getPrice() { return price; }
	public void setPrice(long price) { this.price = price; }
}
