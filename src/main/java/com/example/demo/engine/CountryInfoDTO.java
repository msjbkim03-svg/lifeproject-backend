package com.example.demo.engine;

// C03 진행 화면 나라 패널 (원작 나라 패널): 그해와 가장 가까운 연대의 나라·권역 통계
public class CountryInfoDTO {
	private String countryCode;	// 나라 코드
	private String nameKo;	// 한글 이름
	private String regionName;	// 권역 이름
	private Integer decade;	// 통계 연대
	private Long population;	// 인구
	private Long births;	// 연간 출생아
	private Long deaths;	// 연간 사망자
	private Double lifeExpectancy;	// 기대수명
	private Double infantMortality;	// 영아 사망률 (1,000명당)
	private Double u5Mortality;	// 5세 미만 사망률 (1,000명당)
	private Double gdpPerCapita;	// 1인당 GDP ($)
	private Double urbanRate;	// 도시 거주 (%)
	private Double sexRatioBirth;	// 출생 성비 (여아 1당 남아)
	private Double tfr;	// 합계출산율
	private Double unemploymentRate;	// 실업률 (%)
	private Double netMigrationRate;	// 순이민률 (1,000명당)
	private String isEstimated;	// 추정값이면 Y
	private String sourceTitle;	// 출처
	private Double enrollElem;	// 초등 진학률 (권역, %)
	private Double enrollMiddle;	// 중등 진학률 (권역, %)
	private Double enrollHigh;	// 고등 진학률 (권역, %)
	private Double enrollUni;	// 대학 진학률 (권역, %)
	private Double agriRate;	// 농업 종사 (권역, %)
	private Double industryRate;	// 공업 종사 (권역, %)
	private Double serviceRate;	// 서비스업 종사 (권역, %)

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public String getNameKo() {
		return nameKo;
	}

	public void setNameKo(String nameKo) {
		this.nameKo = nameKo;
	}

	public String getRegionName() {
		return regionName;
	}

	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}

	public Integer getDecade() {
		return decade;
	}

	public void setDecade(Integer decade) {
		this.decade = decade;
	}

	public Long getPopulation() {
		return population;
	}

	public void setPopulation(Long population) {
		this.population = population;
	}

	public Long getBirths() {
		return births;
	}

	public void setBirths(Long births) {
		this.births = births;
	}

	public Long getDeaths() {
		return deaths;
	}

	public void setDeaths(Long deaths) {
		this.deaths = deaths;
	}

	public Double getLifeExpectancy() {
		return lifeExpectancy;
	}

	public void setLifeExpectancy(Double lifeExpectancy) {
		this.lifeExpectancy = lifeExpectancy;
	}

	public Double getInfantMortality() {
		return infantMortality;
	}

	public void setInfantMortality(Double infantMortality) {
		this.infantMortality = infantMortality;
	}

	public Double getU5Mortality() {
		return u5Mortality;
	}

	public void setU5Mortality(Double u5Mortality) {
		this.u5Mortality = u5Mortality;
	}

	public Double getGdpPerCapita() {
		return gdpPerCapita;
	}

	public void setGdpPerCapita(Double gdpPerCapita) {
		this.gdpPerCapita = gdpPerCapita;
	}

	public Double getUrbanRate() {
		return urbanRate;
	}

	public void setUrbanRate(Double urbanRate) {
		this.urbanRate = urbanRate;
	}

	public Double getSexRatioBirth() {
		return sexRatioBirth;
	}

	public void setSexRatioBirth(Double sexRatioBirth) {
		this.sexRatioBirth = sexRatioBirth;
	}

	public Double getTfr() {
		return tfr;
	}

	public void setTfr(Double tfr) {
		this.tfr = tfr;
	}

	public Double getUnemploymentRate() {
		return unemploymentRate;
	}

	public void setUnemploymentRate(Double unemploymentRate) {
		this.unemploymentRate = unemploymentRate;
	}

	public Double getNetMigrationRate() {
		return netMigrationRate;
	}

	public void setNetMigrationRate(Double netMigrationRate) {
		this.netMigrationRate = netMigrationRate;
	}

	public String getIsEstimated() {
		return isEstimated;
	}

	public void setIsEstimated(String isEstimated) {
		this.isEstimated = isEstimated;
	}

	public String getSourceTitle() {
		return sourceTitle;
	}

	public void setSourceTitle(String sourceTitle) {
		this.sourceTitle = sourceTitle;
	}

	public Double getEnrollElem() {
		return enrollElem;
	}

	public void setEnrollElem(Double enrollElem) {
		this.enrollElem = enrollElem;
	}

	public Double getEnrollMiddle() {
		return enrollMiddle;
	}

	public void setEnrollMiddle(Double enrollMiddle) {
		this.enrollMiddle = enrollMiddle;
	}

	public Double getEnrollHigh() {
		return enrollHigh;
	}

	public void setEnrollHigh(Double enrollHigh) {
		this.enrollHigh = enrollHigh;
	}

	public Double getEnrollUni() {
		return enrollUni;
	}

	public void setEnrollUni(Double enrollUni) {
		this.enrollUni = enrollUni;
	}

	public Double getAgriRate() {
		return agriRate;
	}

	public void setAgriRate(Double agriRate) {
		this.agriRate = agriRate;
	}

	public Double getIndustryRate() {
		return industryRate;
	}

	public void setIndustryRate(Double industryRate) {
		this.industryRate = industryRate;
	}

	public Double getServiceRate() {
		return serviceRate;
	}

	public void setServiceRate(Double serviceRate) {
		this.serviceRate = serviceRate;
	}
}
