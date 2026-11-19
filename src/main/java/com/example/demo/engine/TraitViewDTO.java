package com.example.demo.engine;

// C13 특성 칸 (진행 화면): 이 인생의 특성 하나 (LIFE_TRAIT 레벨 + 레벨별 효과값)
//   INVEST 투자 성공 확률 배율, DISEASE 질병 확률 배율, LOVE 연애 이벤트 확률 배율 (1 + 효과값)
//   STUDY 타고난 공부 재능에 더하는 값 (표준편차 단위)
public class TraitViewDTO {
	private String traitId;   // INVEST, DISEASE, LOVE, STUDY
	private int level;        // 1~5
	private Double effect;    // 효과값 (효과 표에 없으면 null)

	public String getTraitId() {
		return traitId;
	}

	public void setTraitId(String traitId) {
		this.traitId = traitId;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public Double getEffect() {
		return effect;
	}

	public void setEffect(Double effect) {
		this.effect = effect;
	}
}
