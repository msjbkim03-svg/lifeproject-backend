package com.example.demo.admin;

import java.util.List;

// A04 관리자 메인 대시보드 (한 번에 보내는 값)
public class DashboardDTO {
	private int todayPlays;	// 오늘 시작된 인생 수 (회원 + 비회원, 보관된 비회원 포함)
	private int totalPlays;	// 누적 인생 수
	private List<CompletionDTO> completion;	// 회원·비회원별 완료율
	private List<QuestRateDTO> quests;	// 퀘스트별 달성률
	private List<TraitLevelDTO> traits;	// 특성별 레벨 분포
	private List<CountryLifeDTO> countries;	// 나라별 평균 수명 vs 실제 기대수명
	private List<DailyPlayDTO> daily;	// 최근 14일 하루 플레이 수 (11/25)
	private List<DeathCauseDTO> causes;	// 사망 원인 분포 (11/25)
	private List<AgeBucketDTO> ages;	// 수명 분포 10년 단위 (11/25)
	private List<RecentLifeDTO> recent;	// 최근 끝난 인생 (11/25)

	public int getTodayPlays() {
		return todayPlays;
	}

	public void setTodayPlays(int todayPlays) {
		this.todayPlays = todayPlays;
	}

	public int getTotalPlays() {
		return totalPlays;
	}

	public void setTotalPlays(int totalPlays) {
		this.totalPlays = totalPlays;
	}

	public List<CompletionDTO> getCompletion() {
		return completion;
	}

	public void setCompletion(List<CompletionDTO> completion) {
		this.completion = completion;
	}

	public List<QuestRateDTO> getQuests() {
		return quests;
	}

	public void setQuests(List<QuestRateDTO> quests) {
		this.quests = quests;
	}

	public List<TraitLevelDTO> getTraits() {
		return traits;
	}

	public void setTraits(List<TraitLevelDTO> traits) {
		this.traits = traits;
	}

	public List<CountryLifeDTO> getCountries() {
		return countries;
	}

	public void setCountries(List<CountryLifeDTO> countries) {
		this.countries = countries;
	}

	public List<DailyPlayDTO> getDaily() {
		return daily;
	}

	public void setDaily(List<DailyPlayDTO> daily) {
		this.daily = daily;
	}

	public List<DeathCauseDTO> getCauses() {
		return causes;
	}

	public void setCauses(List<DeathCauseDTO> causes) {
		this.causes = causes;
	}

	public List<AgeBucketDTO> getAges() {
		return ages;
	}

	public void setAges(List<AgeBucketDTO> ages) {
		this.ages = ages;
	}

	public List<RecentLifeDTO> getRecent() {
		return recent;
	}

	public void setRecent(List<RecentLifeDTO> recent) {
		this.recent = recent;
	}
}
