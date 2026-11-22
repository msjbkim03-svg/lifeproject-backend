package com.example.demo.member;

import java.util.List;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;


@Repository
public class MemberDAO {

	private final SqlSessionTemplate mybatis;

	public MemberDAO(SqlSessionTemplate mybatis) {
		this.mybatis = mybatis;
	}

	// 아이디 중복 확인 → 같은 아이디 개수 (0이면 사용 가능)
	public int countByLoginId(String loginId) {
		return mybatis.selectOne("member.countByLoginId", loginId);
	}

	// 닉네임 중복 확인
	public int countByNickname(String nickname) {
		return mybatis.selectOne("member.countByNickname", nickname);
	}

	// 이메일 중복 확인
	public int countByEmail(String email) {
		return mybatis.selectOne("member.countByEmail", email);
	}

	// 회원 1줄 INSERT (실행 후 dto.memberId 에 새 번호가 채워짐)
	public int insertMember(MemberDTO dto) {
		return mybatis.insert("member.insertMember", dto);
	}

	// 새 회원의 특성 4종(INVEST, DISEASE, LOVE, STUDY)을 Lv.1로 INSERT
	public int insertDefaultTraits(int memberId) {
		return mybatis.insert("member.insertDefaultTraits", memberId);
	}

	// 로그인: 아이디로 회원 1명 찾기 (없으면 null)
	public MemberDTO selectByLoginId(String loginId) {
		return mybatis.selectOne("member.selectByLoginId", loginId);
	}

	// 이 회원의 진행 중 인생 개수 (0 또는 1)
	public int countPlayingLife(int memberId) {
		return mybatis.selectOne("member.countPlayingLife", memberId);
	}

	// 비회원 인생에 회원 번호 채우기 → 바뀐 줄 수 (0이면 전환된 인생 없음)
	public int convertGuestLife(int memberId, String guestToken) {
		return mybatis.update("member.convertGuestLife",
				Map.of("memberId", memberId, "guestToken", guestToken));
	}

	// 마이페이지 (M04): 회원 1명의 마이페이지 정보 (없거나 탈퇴했으면 null)
	public MyPageDTO selectMyPage(int memberId) {
		return mybatis.selectOne("member.selectMyPage", memberId);
	}

	// M05·M06: 회원 번호로 1명 찾기 (비밀번호 비교용)
	public MemberDTO selectById(int memberId) {
		return mybatis.selectOne("member.selectById", memberId);
	}

	// M05: 나를 뺀 같은 닉네임 개수 (0이면 사용 가능)
	public int countNicknameExceptMe(int memberId, String nickname) {
		return mybatis.selectOne("member.countNicknameExceptMe",
				Map.of("memberId", memberId, "nickname", nickname));
	}

	// M05: 나를 뺀 같은 이메일 개수
	public int countEmailExceptMe(int memberId, String email) {
		return mybatis.selectOne("member.countEmailExceptMe",
				Map.of("memberId", memberId, "email", email));
	}

	// M05: 닉네임·이메일(·새 비밀번호) 저장
	public int updateMember(MemberUpdateDTO dto) {
		return mybatis.update("member.updateMember", dto);
	}

	// M06: 탈퇴 처리 (STATUS = 'WITHDRAWN')
	public int updateWithdrawn(int memberId) {
		return mybatis.update("member.updateWithdrawn", memberId);
	}
	
	// ===== 아이디·비밀번호 찾기 (M09) =====

	// 1분 안에 만든 인증번호 개수 (0이면 보내도 됨)
	public int countRecentAuth(String email, String purpose) {
		return mybatis.selectOne("member.countRecentAuth",
				Map.of("email", email, "purpose", purpose));
	}

	// 인증번호 저장 (유효시간 5분은 SQL 안에서 계산)
	public int insertEmailAuth(String email, String code, String purpose) {
		return mybatis.insert("member.insertEmailAuth",
				Map.of("email", email, "code", code, "purpose", purpose));
	}

	// 이메일로 회원 찾기 (탈퇴 회원은 제외, 없으면 null)
	public MemberDTO selectActiveByEmail(String email) {
		return mybatis.selectOne("member.selectActiveByEmail", email);
	}

	// 아이디 + 이메일로 회원 찾기 (탈퇴 회원은 제외, 없으면 null)
	public MemberDTO selectActiveByLoginIdAndEmail(String loginId, String email) {
		return mybatis.selectOne("member.selectActiveByLoginIdAndEmail",
				Map.of("loginId", loginId, "email", email));
	}

	// 가장 최근 인증번호가 지금도 쓸 수 있는지 (1이면 통과, 0이면 실패)
	public int countValidAuth(String email, String purpose, String code) {
		return mybatis.selectOne("member.countValidAuth",
				Map.of("email", email, "purpose", purpose, "code", code));
	}

	// 가장 최근 인증번호를 '사용함'(IS_USED = 'Y')으로 바꾸기
	public int updateAuthUsed(String email, String purpose) {
		return mybatis.update("member.updateAuthUsed",
				Map.of("email", email, "purpose", purpose));
	}

	// 틀렸을 때 가장 최근 인증번호의 틀린 횟수 +1
	public int increaseAuthFail(String email, String purpose) {
		return mybatis.update("member.increaseAuthFail",
				Map.of("email", email, "purpose", purpose));
	}

	// 비밀번호 바꾸기 (encodedPw = BCrypt 로 바꾼 값)
	public int updatePassword(int memberId, String encodedPw) {
		return mybatis.update("member.updatePassword",
				Map.of("memberId", memberId, "password", encodedPw));
	}

	// C01·M08: 내 특성 4종 레벨
	public List<MemberTraitDTO> selectMyTraits(int memberId) {
		return mybatis.selectList("member.selectMyTraits", memberId);
	}

	// M07: 끝난 인생 목록 (start 다음 줄부터 end 줄까지)
	public List<LifeRecordDTO> selectMyLives(int memberId, String sort, int start, int end) {
		return mybatis.selectList("member.selectMyLives",
				Map.of("memberId", memberId, "sort", sort, "start", start, "end", end));
	}

	// M07: 끝난 인생 개수
	public int countMyEndedLives(int memberId) {
		return mybatis.selectOne("member.countMyEndedLives", memberId);
	}

	// M07: 퀘스트 달성 수 (내 모든 인생)
	public int countMyQuestDone(int memberId) {
		return mybatis.selectOne("member.countMyQuestDone", memberId);
	}

	// M07: 퀘스트로 받은 포인트 합계
	public int sumMyQuestPoint(int memberId) {
		return mybatis.selectOne("member.sumMyQuestPoint", memberId);
	}

	// M08: 특성 레벨 +1 (최대 레벨 미만일 때만) → 바뀐 줄 수
	public int increaseTraitLevel(int memberId, String traitId) {
		return mybatis.update("member.increaseTraitLevel", Map.of("memberId", memberId, "traitId", traitId));
	}

	// M08: 포인트 -1 (1점 이상일 때만) → 바뀐 줄 수
	public int decreasePoint(int memberId) {
		return mybatis.update("member.decreasePoint", memberId);
	}

	// M08: 포인트 내역 저장 (-1, TRAIT_LEVELUP)
	public int insertTraitLevelUpHistory(int memberId) {
		return mybatis.insert("member.insertTraitLevelUpHistory", memberId);
	}
}
