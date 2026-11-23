package com.example.demo.admin;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

// A01 관리자 회원 관리 - 검색·페이지 계산, 강제 탈퇴 검사
@Service
public class AdminMemberService {

	private static final int SIZE = 10; // 한 페이지에 10명

	private final AdminMemberDAO dao;

	public AdminMemberService(AdminMemberDAO dao) {
		this.dao = dao;
	}

	// ===== 회원 목록 =====
	// keyword: 아이디·닉네임·이메일 중 하나라도 들어 있으면 / status: ACTIVE, WITHDRAWN, 그 밖은 전체
	public Map<String, Object> list(String keyword, String status, int page) {
		// 1. 이상한 값 정리 (빈칸 검색은 검색 안 한 것으로)
		if (page < 1) page = 1;
		if (keyword != null) keyword = keyword.trim();
		if (keyword != null && keyword.isEmpty()) keyword = null;
		// % _ 를 "아무 글자"가 아니라 진짜 글자로 찾게 앞에 \ 붙이기 (SQL 의 ESCAPE '\')
		if (keyword != null) keyword = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
		if (!"ACTIVE".equals(status) && !"WITHDRAWN".equals(status)) status = null;

		// 2. SQL 에 넘길 조건 상자
		//    Map.of 는 null 값을 못 넣어서(에러) HashMap 을 씀 → null 이면 SQL 의 <if> 가 그 조건을 뺌
		Map<String, Object> cond = new HashMap<>();
		cond.put("keyword", keyword);
		cond.put("status", status);
		cond.put("start", (page - 1) * SIZE);
		cond.put("end", page * SIZE);

		// 3. 화면에 보낼 상자
		Map<String, Object> result = new HashMap<>();
		result.put("total", dao.countMembers(cond));
		result.put("page", page);
		result.put("size", SIZE);
		result.put("list", dao.selectMembers(cond));
		return result;
	}

	// ===== 강제 탈퇴 =====
	// 문제가 있으면 오류 문구, 성공하면 null
	// 회원이 스스로 탈퇴(M06)할 때와 똑같이 STATUS 만 바꾸고 전화번호를 지움 (인생·추모관 기록은 남김)
	public String withdraw(int memberId) {
		AdminMemberDTO m = dao.selectMember(memberId);
		if (m == null) return "없는 회원이에요.";
		if ("ADMIN".equals(m.getMemberRole())) return "관리자 계정은 탈퇴시킬 수 없어요.";
		if ("WITHDRAWN".equals(m.getStatus())) return "이미 탈퇴한 회원이에요.";

		dao.updateWithdrawn(memberId);
		return null;
	}
}
