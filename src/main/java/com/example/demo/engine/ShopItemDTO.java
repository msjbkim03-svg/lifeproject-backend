package com.example.demo.engine;

// C06-3 돈 쓰기·투자 한 줄 (원작 '돈 쓰기 · 투자' 창)
//   kind: BUY 물건 사기 / DO 돈 쓰기(여행·용돈 …) / INVEST 여유 자금 굴리기
public class ShopItemDTO {
	private String id;      // 항목 코드 (BUY_CAR, DO_TRIP, INV_STOCK …)
	private String kind;    // BUY, DO, INVEST
	private String name;    // 화면 이름
	private String desc;    // 효과·설명 한 줄
	private long price;     // 값 (구매력 달러, 화면은 한국 체감 원) / 투자는 0
	private boolean ok;     // 지금 고를 수 있는지
	private String why;     // 못 고르는 이유
	private boolean done;   // 이미 가진 물건 · 올해 이미 한 일 · 지금 쓰는 투자 방식

	public ShopItemDTO() {
	}

	public ShopItemDTO(String id, String kind, String name, String desc, long price) {
		this.id = id;
		this.kind = kind;
		this.name = name;
		this.desc = desc;
		this.price = price;
		this.ok = true;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public long getPrice() {
		return price;
	}

	public void setPrice(long price) {
		this.price = price;
	}

	public boolean isOk() {
		return ok;
	}

	public void setOk(boolean ok) {
		this.ok = ok;
	}

	public String getWhy() {
		return why;
	}

	public void setWhy(String why) {
		this.why = why;
	}

	public boolean isDone() {
		return done;
	}

	public void setDone(boolean done) {
		this.done = done;
	}
}
