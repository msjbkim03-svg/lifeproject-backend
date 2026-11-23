package com.example.demo.costume;

// 옷 가게가 쓰는 인생 정보 (주인 확인 + 돈 + 그 나라 1인당 GDP)
public class CostumeLifeDTO {
	private int lifeId;
	private Integer memberId;
	private String guestToken;
	private String status;
	private int currentAge;
	private long assets;
	private Double gdpPerCapita;

	public int getLifeId() { return lifeId; }
	public void setLifeId(int lifeId) { this.lifeId = lifeId; }
	public Integer getMemberId() { return memberId; }
	public void setMemberId(Integer memberId) { this.memberId = memberId; }
	public String getGuestToken() { return guestToken; }
	public void setGuestToken(String guestToken) { this.guestToken = guestToken; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
	public int getCurrentAge() { return currentAge; }
	public void setCurrentAge(int currentAge) { this.currentAge = currentAge; }
	public long getAssets() { return assets; }
	public void setAssets(long assets) { this.assets = assets; }
	public Double getGdpPerCapita() { return gdpPerCapita; }
	public void setGdpPerCapita(Double gdpPerCapita) { this.gdpPerCapita = gdpPerCapita; }
}
