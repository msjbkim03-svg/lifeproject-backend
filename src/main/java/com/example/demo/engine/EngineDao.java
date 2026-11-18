package com.example.demo.engine;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EngineDao {
	String selectNow();

	// C02 출생
	List<CountryStatDTO> selectBirthCountries(int decade);
	String selectRandomName(LifeDTO life);
	String selectHistName(LifeDTO life);
	int selectNextLifeId();
	int abandonGuestLife(String guestToken);
	int abandonMemberLife(int memberId);
	int moveOldGuestStats();
	int deleteOldGuestLives();
	int insertLife(LifeDTO life);

	// C03 1년 진행
	LifeDTO selectLife(int lifeId);
	LifeDTO selectPlayingLife(LifeDTO cond);
	int updateLifeYear(LifeDTO life);
	int insertYearLog(LifeDTO life);

	// C07 사망 판정
	CountryStatDTO selectYearStat(LifeDTO life);
	int endLife(LifeDTO life);
	List<DeathCauseDTO> selectDeathCauses(DeathCauseDTO cond);
	Long selectAvgIncome(LifeDTO life);
	Double selectPoorerPct(LifeDTO life);

	// C04 집중 선택
	int updateFocus(LifeDTO life);

	// C05 경험
	int insertExperience(ExperienceDTO exp);

	// 동시에 같은 해를 두 번 진행·같은 선택을 두 번 반영하지 않게 잠금 (0이면 이미 처리됨)
	int lockLifeYear(LifeDTO life);
	int lockPending(LifeDTO life);

	// C10 → C06 역사 사건 보정값 (실직·학업 중단·피란)
	HistEffectDTO selectHistEffect(String eventId);

	// C13 특성: 태어날 때 회원 특성 레벨 복사, 학업 능력 효과값
	int insertLifeTraits(LifeDTO life);
	Double selectStudyEffect(int lifeId);

	// C09 이벤트 기록
	int insertEventLog(EventLogDTO log);
	String selectPendingText(LifeDTO life);
	int countPendingChosen(LifeDTO life); // C10: 선택 이벤트에서 이미 '떠난다'를 골랐는지 (목적지 기다리는 중)

	// C06 진학
	RegionStatDTO selectRegionStat(LifeDTO life);

	// C06 직업
	List<JobDTO> selectJobs();
	JobDTO selectJob(String jobId);
}
