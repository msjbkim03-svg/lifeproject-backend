package com.example.demo.admin;

import java.util.List;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

// A01 관리자 회원 관리 DB 접근 (SQL 은 resources/mappers/admin-member-mapper.xml, namespace "adminMember")
@Repository
public class AdminMemberDAO {

	private final SqlSessionTemplate mybatis;

	public AdminMemberDAO(SqlSessionTemplate mybatis) {
		this.mybatis = mybatis;
	}

	// 검색 조건에 맞는 회원 목록 (start 다음 줄부터 end 줄까지)
	public List<AdminMemberDTO> selectMembers(Map<String, Object> cond) {
		return mybatis.selectList("adminMember.selectMembers", cond);
	}

	// 검색 조건에 맞는 회원 수
	public int countMembers(Map<String, Object> cond) {
		return mybatis.selectOne("adminMember.countMembers", cond);
	}

	// 회원 1명 (권한·상태 확인용, 없으면 null)
	public AdminMemberDTO selectMember(int memberId) {
		return mybatis.selectOne("adminMember.selectMember", memberId);
	}

	// 강제 탈퇴 → 바뀐 줄 수
	public int updateWithdrawn(int memberId) {
		return mybatis.update("adminMember.updateWithdrawn", memberId);
	}
}
