package com.example.demo.engine;

// 역사 사건 1건의 엔진용 보정값 (GAME_EVENT 강도 + HIST_EVENT_TYPE 유형 효과, C10 → C06 반영)
public class HistEffectDTO {
	private Integer intensity;	// 강도 (GAME_EVENT.INTENSITY, 없으면 1)
	private Double jobLossAdd;	// 강도 1 기준 실직 확률
	private Double eduStopAdd;	// 강도 1 기준 학업 중단 확률
	private Double migrateAdd;	// 강도 1 기준 이민(피란) 확률
	private String typeName;	// 유형 이름 (전쟁, 경제 위기 등)
	private String typeCode;	// 유형 코드 WAR, ECON, DISASTER, POLITICS, REFUGE

	public String getTypeCode() {
		return typeCode;
	}

	public void setTypeCode(String typeCode) {
		this.typeCode = typeCode;
	}

	public Integer getIntensity() {
		return intensity;
	}

	public void setIntensity(Integer intensity) {
		this.intensity = intensity;
	}

	public Double getJobLossAdd() {
		return jobLossAdd;
	}

	public void setJobLossAdd(Double jobLossAdd) {
		this.jobLossAdd = jobLossAdd;
	}

	public Double getEduStopAdd() {
		return eduStopAdd;
	}

	public void setEduStopAdd(Double eduStopAdd) {
		this.eduStopAdd = eduStopAdd;
	}

	public Double getMigrateAdd() {
		return migrateAdd;
	}

	public void setMigrateAdd(Double migrateAdd) {
		this.migrateAdd = migrateAdd;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}
}
