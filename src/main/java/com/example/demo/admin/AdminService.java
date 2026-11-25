package com.example.demo.admin;

import org.springframework.stereotype.Service;

@Service
public class AdminService {

	private final AdminDao adminDao;

	public AdminService(AdminDao adminDao) {
		this.adminDao = adminDao;
	}

	// 관리자(ADMIN)인지 확인 - 로그인 안 했거나 일반 회원이면 false
	public boolean isAdmin(Integer memberId) {
		if (memberId == null) {
			return false;
		}
		return "ADMIN".equals(adminDao.selectRole(memberId));
	}

	// ================= A04 관리자 메인 =================
	// 대시보드 값 한 번에 모으기
	// 비회원 인생은 지워지기 전에 GUEST_LIFE_STAT 에 숫자만 옮겨 두므로 LIFE + GUEST_LIFE_STAT 를 합쳐서 셈
	public DashboardDTO dashboard() {
		DashboardDTO d = new DashboardDTO();
		d.setTodayPlays(adminDao.countTodayPlays());
		d.setTotalPlays(adminDao.countTotalPlays());
		d.setCompletion(adminDao.selectCompletion());
		d.setQuests(adminDao.selectQuestRates());
		d.setTraits(adminDao.selectTraitLevels());
		d.setCountries(adminDao.selectCountryLife());
		d.setDaily(adminDao.selectDailyPlays());
		d.setCauses(adminDao.selectDeathCauses());
		d.setAges(adminDao.selectAgeBuckets());
		d.setRecent(adminDao.selectRecentEnded(8));
		return d;
	}
}
