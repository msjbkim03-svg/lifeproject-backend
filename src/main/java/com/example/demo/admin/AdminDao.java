package com.example.demo.admin;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

// A04 관리자 메인 집계 (쿼리는 resources/mappers/admin-mapper.xml)
@Mapper
public interface AdminDao {
	String selectRole(int memberId);					// 관리자인지 확인 (ADMIN / USER)
	int countTodayPlays();
	int countTotalPlays();
	List<CompletionDTO> selectCompletion();
	List<QuestRateDTO> selectQuestRates();
	List<TraitLevelDTO> selectTraitLevels();
	List<CountryLifeDTO> selectCountryLife();
	List<DailyPlayDTO> selectDailyPlays();			// 최근 14일 (11/25)
	List<DeathCauseDTO> selectDeathCauses();		// 사망 원인 분포 (11/25)
	List<AgeBucketDTO> selectAgeBuckets();			// 수명 분포 (11/25)
	List<RecentLifeDTO> selectRecentEnded(int n);	// 최근 끝난 인생 n개 (11/25)
}
