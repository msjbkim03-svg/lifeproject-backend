package com.example.demo.engine;

public class JobDTO {
	private String jobId;	// 직업 코드 (예: JOB_DOCTOR)
	private String jobName;	// 직업명
	private String requiredEdu;	// 필요 학력 NONE, ELEM, MIDDLE, HIGH, UNI, GRAD
	private String industry;	// 산업 AGRI, INDUSTRY, SERVICE
	private double incomeRate;	// 소득 배율
	private Long yearIncome;	// [화면용] 이 일을 하면 받을 1년 소득 (구매력 달러, 그 나라 그해 1인당 GDP × 배율)

	public Long getYearIncome() {
		return yearIncome;
	}

	public void setYearIncome(Long yearIncome) {
		this.yearIncome = yearIncome;
	}

	// 직업 목록은 서버 메모리에 한 벌만 있어서, 인생마다 다른 값(yearIncome)을 넣을 땐 복사본에
	public JobDTO copy() {
		JobDTO j = new JobDTO();
		j.jobId = jobId;
		j.jobName = jobName;
		j.requiredEdu = requiredEdu;
		j.industry = industry;
		j.incomeRate = incomeRate;
		return j;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public String getRequiredEdu() {
		return requiredEdu;
	}

	public void setRequiredEdu(String requiredEdu) {
		this.requiredEdu = requiredEdu;
	}

	public String getIndustry() {
		return industry;
	}

	public void setIndustry(String industry) {
		this.industry = industry;
	}

	public double getIncomeRate() {
		return incomeRate;
	}

	public void setIncomeRate(double incomeRate) {
		this.incomeRate = incomeRate;
	}

}
