package com.example.demo.member;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.internet.MimeMessage;

// 실제 일(검사, 암호화, 저장)을 하는 곳
@Service
public class MemberService {

	private MemberDAO dao;
	private JavaMailSender mailSender; // 메일 보내는 도구 (M09)
	private String mailFrom;           // 보내는 사람 = Gmail 주소 (spring.mail.username)
	private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

	public MemberService(MemberDAO dao, JavaMailSender mailSender,
			@Value("${spring.mail.username}") String mailFrom) {
		this.dao = dao;
		this.mailSender = mailSender;
		this.mailFrom = mailFrom;
	}

	// ===== 중복 확인 (true = 사용 가능) =====
	public boolean isLoginIdAvailable(String loginId) {
		return dao.countByLoginId(loginId) == 0;
	}

	public boolean isNicknameAvailable(String nickname) {
		return dao.countByNickname(nickname) == 0;
	}

	public boolean isEmailAvailable(String email) {
		return dao.countByEmail(email) == 0;
	}

	// ===== 회원가입 (M01) =====
	// 문제가 있으면 오류 문구를 돌려주고, 성공하면 null 을 돌려준다
	@Transactional // 회원 INSERT 와 특성 INSERT 중 하나라도 실패하면 둘 다 취소
	public String join(MemberDTO dto) {

		// 1. 입력 형식 검사 (화면에서도 검사하지만 서버에서 한 번 더)
		if (dto.getLoginId() == null || !dto.getLoginId().matches("^[a-zA-Z0-9]{4,20}$")) {
			return "아이디는 영문·숫자 4~20자로 입력해 주세요.";
		}
		if (dto.getPassword() == null
				|| !dto.getPassword().matches("^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9]).{8,20}$")) {
			return "비밀번호는 영문·숫자·특수문자를 포함해 8~20자로 입력해 주세요.";
		}
		if (!dto.getPassword().equals(dto.getPasswordCheck())) {
			return "비밀번호 확인이 일치하지 않습니다.";
		}
		if (dto.getNickname() == null || dto.getNickname().length() < 2 || dto.getNickname().length() > 10) {
			return "닉네임은 2~10자로 입력해 주세요.";
		}
		if (dto.getEmail() == null || !dto.getEmail().matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
			return "이메일 형식이 올바르지 않습니다.";
		}
		// 전화번호: 010-1234-5678 처럼 와도 숫자만 남겨서 검사·저장
		String phone = dto.getPhone() == null ? "" : dto.getPhone().replaceAll("[^0-9]", "");
		if (!phone.matches("^01[0-9]{8,9}$")) {
			return "전화번호를 올바르게 입력해 주세요. (예: 010-1234-5678)";
		}
		dto.setPhone(phone);
		// 개인정보 활용 동의는 필수
		if (!"Y".equals(dto.getPrivacyAgreed())) {
			return "개인정보 수집·이용에 동의해 주세요.";
		}

		// 2. 중복 검사
		if (!isLoginIdAvailable(dto.getLoginId())) return "이미 사용 중인 아이디입니다.";
		if (!isNicknameAvailable(dto.getNickname())) return "이미 사용 중인 닉네임입니다.";
		if (!isEmailAvailable(dto.getEmail())) return "이미 사용 중인 이메일입니다.";

		// 3. 비밀번호를 BCrypt 로 바꿔서 담기 (원래 비밀번호는 저장 안 함)
		dto.setPassword(encoder.encode(dto.getPassword()));

		// 4. MEMBER 에 1줄 저장 → dto.memberId 에 새 번호가 들어옴
		dao.insertMember(dto);

		// 5. 그 번호로 특성 4종 Lv.1 저장
		dao.insertDefaultTraits(dto.getMemberId());

		return null; // 성공
	}

	// ===== 로그인 (M02) =====
	// 아이디·비밀번호가 맞으면 회원 정보를, 틀리면 null 을 돌려준다
	public MemberDTO login(String loginId, String password) {

		// 0. 빈 값이면 바로 실패
		if (loginId == null || password == null) return null;

		// 1. 아이디로 회원 찾기
		MemberDTO member = dao.selectByLoginId(loginId);
		if (member == null) return null; // 없는 아이디

		// 2. 탈퇴한 회원은 로그인 불가
		if ("WITHDRAWN".equals(member.getStatus())) return null;

		// 3. 입력한 비밀번호를 DB의 BCrypt 값과 비교
		if (!encoder.matches(password, member.getPassword())) return null;

		return member; // 성공
	}

	// ===== 비회원 인생 → 회원 인생 전환 (M02) =====
	// 전환했으면 true, 안 했으면 false
	public boolean convertGuestLife(int memberId, String guestToken) {

		// 1. 비회원으로 하던 인생이 없으면 할 일 없음
		if (guestToken == null || guestToken.isBlank()) return false;

		// 2. 회원에게 이미 진행 중인 인생이 있으면 전환하지 않음
		//    (회원당 진행 중 인생 1개 규칙, 남은 비회원 인생은 7일 뒤 자동 삭제)
		if (dao.countPlayingLife(memberId) > 0) return false;

		// 3. 비회원 인생에 회원 번호 채우기
		return dao.convertGuestLife(memberId, guestToken) > 0;
	}

	// ===== 마이페이지 (M04) =====
	// 회원 정보 + 포인트 + 살아본 인생 수 + 퀘스트 달성 수 + 진행 중인 인생 (없으면 null)
	public MyPageDTO myPage(int memberId) {
		return dao.selectMyPage(memberId);
	}

	// ===== 개인정보 수정 (M05) =====
	// 문제가 있으면 오류 문구를, 성공하면 null 을 돌려준다
	@Transactional // 인증번호 '사용함' 처리와 회원 정보 저장을 한 번에 (하나라도 실패하면 둘 다 취소)
	public String update(int memberId, MemberUpdateDTO dto) {

		// 1. DB 에서 지금 내 정보 꺼내기 (비밀번호 비교용)
		MemberDTO me = dao.selectById(memberId);
		if (me == null || "WITHDRAWN".equals(me.getStatus())) return "회원 정보를 찾을 수 없습니다.";

		// 2. 현재 비밀번호 확인 (BCrypt 비교)
		if (dto.getCurrentPassword() == null || !encoder.matches(dto.getCurrentPassword(), me.getPassword())) {
			return "현재 비밀번호가 올바르지 않습니다.";
		}

		// 3. 닉네임·이메일 형식 검사 (회원가입과 같은 규칙)
		if (dto.getNickname() == null || dto.getNickname().length() < 2 || dto.getNickname().length() > 10) {
			return "닉네임은 2~10자로 입력해 주세요.";
		}
		if (dto.getEmail() == null || !dto.getEmail().matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
			return "이메일 형식이 올바르지 않습니다.";
		}
		// 전화번호 (회원가입과 같은 규칙, 숫자만 남겨서 저장)
		String phone = dto.getPhone() == null ? "" : dto.getPhone().replaceAll("[^0-9]", "");
		if (!phone.matches("^01[0-9]{8,9}$")) {
			return "전화번호를 올바르게 입력해 주세요. (예: 010-1234-5678)";
		}
		dto.setPhone(phone);

		// 4. 중복 검사 (나 자신은 빼고 셈 → 닉네임 그대로 두면 통과)
		if (dao.countNicknameExceptMe(memberId, dto.getNickname()) > 0) return "이미 사용 중인 닉네임입니다.";
		if (dao.countEmailExceptMe(memberId, dto.getEmail()) > 0) return "이미 사용 중인 이메일입니다.";

		// 5. 새 비밀번호: 비워 두면 안 바꿈(null), 입력했으면 검사 후 BCrypt 로 바꿔서 담기
		String newPw = dto.getNewPassword();
		if (newPw == null || newPw.isBlank()) {
			dto.setNewPassword(null);
		} else {
			if (!newPw.matches("^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9]).{8,20}$")) {
				return "새 비밀번호는 영문·숫자·특수문자를 포함해 8~20자로 입력해 주세요.";
			}
			if (!newPw.equals(dto.getNewPasswordCheck())) {
				return "새 비밀번호 확인이 일치하지 않습니다.";
			}
			dto.setNewPassword(encoder.encode(newPw));
		}

		// 6. 이메일을 바꿨으면 새 이메일로 받은 인증번호 확인 (안 바꿨으면 건너뜀)
		if (!dto.getEmail().equals(me.getEmail())) {
			if (!verifyCode(dto.getEmail(), "CHANGE_EMAIL", dto.getEmailCode())) {
				return "이메일 인증번호가 올바르지 않거나 만료되었습니다.";
			}
		}

		// 7. 저장 (아이디는 바꾸지 않음)
		dto.setMemberId(memberId);
		dao.updateMember(dto);
		return null; // 성공
	}

	// ===== 회원 탈퇴 (M06) =====
	// 비밀번호가 맞으면 STATUS 를 'WITHDRAWN' 으로 바꾸고 true
	// (회원 줄은 지우지 않음 → 인생·추모관 글과 외래키 오류 방지)
	public boolean withdraw(int memberId, String password) {
		MemberDTO me = dao.selectById(memberId);
		if (me == null || "WITHDRAWN".equals(me.getStatus())) return false;
		if (password == null || !encoder.matches(password, me.getPassword())) return false;

		return dao.updateWithdrawn(memberId) > 0;
	}

	// ===== 인증번호 보내기 (M09) =====
	// 문제가 있으면 오류 문구를, 성공하면 null 을 돌려준다
	// ※ 가입된 이메일이든 아니든 겉으로는 똑같이 "성공"처럼 보이게 함 (가입 여부를 알 수 없게)
	public String sendCode(MemberFindDTO dto) {

		// 1. 입력 검사
		String purpose = dto.getPurpose();
		if (!"FIND_ID".equals(purpose) && !"RESET_PW".equals(purpose)) {
			return "잘못된 요청입니다.";
		}
		if (dto.getEmail() == null || !dto.getEmail().matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
			return "이메일 형식이 올바르지 않습니다.";
		}
		if ("RESET_PW".equals(purpose) && (dto.getLoginId() == null || dto.getLoginId().isBlank())) {
			return "아이디를 입력해 주세요.";
		}

		// 2. 재발송은 1분 뒤부터 (EMAIL_AUTH 에 1분 안에 만든 번호가 있으면 막기)
		if (dao.countRecentAuth(dto.getEmail(), purpose) > 0) {
			return "인증번호는 1분 뒤에 다시 받을 수 있어요.";
		}

		// 3. 6자리 랜덤 숫자 만들기 (000123 처럼 앞이 0이어도 6자리)
		String code = String.format("%06d", new SecureRandom().nextInt(1000000));

		// 4. DB 에 저장 (유효시간 5분)
		//    회원이 없어도 저장함 → 1분 제한이 똑같이 걸려서 "가입된 이메일인지" 알 수 없음
		dao.insertEmailAuth(dto.getEmail(), code, purpose);

		// 5. 일치하는 회원이 있을 때만 메일 발송
		MemberDTO member = "FIND_ID".equals(purpose)
				? dao.selectActiveByEmail(dto.getEmail())
				: dao.selectActiveByLoginIdAndEmail(dto.getLoginId(), dto.getEmail());

		if (member != null) {
			try {
				sendCodeMail(dto.getEmail(), code, purpose);
			} catch (Exception e) {
				// 메일이 안 나가도 화면에는 똑같은 문구를 보여 줌 (가입 여부 노출 방지)
				// 원인은 STS 콘솔에서 확인 (앱 비밀번호가 틀렸는지 등)
				e.printStackTrace();
			}
		}
		return null; // 성공
	}

	// ===== 인증번호 확인 (아이디 찾기·비밀번호 재설정이 같이 씀) =====
	// 맞으면 true (그 번호는 '사용함'으로 바꿈), 틀리면 false (틀린 횟수 +1)
	private boolean verifyCode(String email, String purpose, String code) {
		if (email == null || code == null) return false;

		// 가장 최근 번호가 [같은 번호 · 안 씀 · 5분 안 지남 · 틀린 횟수 5번 미만] 이면 통과
		if (dao.countValidAuth(email, purpose, code) > 0) {
			dao.updateAuthUsed(email, purpose);
			return true;
		}
		dao.increaseAuthFail(email, purpose); // 5번 틀리면 이 번호는 더 이상 못 씀
		return false;
	}

	// ===== 아이디 찾기 (M09) =====
	// 인증번호가 맞으면 일부 가린 아이디(msj***)를, 틀리면 null 을 돌려준다
	@Transactional
	public String findId(String email, String code) {
		if (!verifyCode(email, "FIND_ID", code)) return null;

		MemberDTO member = dao.selectActiveByEmail(email);
		if (member == null) return null;

		// 앞 3글자만 보여 주고 나머지는 *** (아이디는 4자 이상이라 안전)
		return member.getLoginId().substring(0, 3) + "***";
	}

	// ===== 비밀번호 찾기 - 인증번호 먼저 확인 (M09) =====
	// 맞으면 null, 틀리면 오류 문구
	// 번호를 '사용함'으로 바꾸지 않음 → 비밀번호를 바꿀 때(resetPw) 한 번 더 확인하고 그때 사용 처리
	public String checkResetCode(MemberFindDTO dto) {
		String fail = "인증번호가 올바르지 않거나 만료되었습니다.";
		if (dto.getEmail() == null || dto.getCode() == null) return fail;

		// 1. 아이디·이메일이 맞는 회원인지 먼저 (안 맞아도 똑같은 문구 → 가입 여부 안 드러남)
		MemberDTO member = dao.selectActiveByLoginIdAndEmail(dto.getLoginId(), dto.getEmail());

		// 2. 회원이 없거나 번호가 틀리면 틀린 횟수 +1 (5번 틀리면 그 번호는 못 씀)
		if (member == null || dao.countValidAuth(dto.getEmail(), "RESET_PW", dto.getCode()) == 0) {
			dao.increaseAuthFail(dto.getEmail(), "RESET_PW");
			return fail;
		}
		return null; // 인증 성공
	}

	// ===== 비밀번호 재설정 (M09) =====
	// 문제가 있으면 오류 문구를, 성공하면 null 을 돌려준다
	@Transactional // 번호를 '사용함'으로 바꾸는 것과 비밀번호 저장 중 하나라도 실패하면 둘 다 취소
	public String resetPassword(MemberFindDTO dto) {

		// 1. 새 비밀번호 검사 (먼저 검사해야 형식 실수로 인증번호가 헛되이 쓰이지 않음)
		String newPw = dto.getNewPassword();
		if (newPw == null || !newPw.matches("^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9]).{8,20}$")) {
			return "비밀번호는 영문·숫자·특수문자를 포함해 8~20자로 입력해 주세요.";
		}
		if (!newPw.equals(dto.getNewPasswordCheck())) {
			return "비밀번호 확인이 일치하지 않습니다.";
		}

		// 2. 아이디와 이메일이 맞는 회원 찾기 (먼저 → 안 맞으면 인증번호를 쓰지 않음)
		String fail = "인증번호가 올바르지 않거나 만료되었습니다.";
		MemberDTO member = dao.selectActiveByLoginIdAndEmail(dto.getLoginId(), dto.getEmail());
		if (member == null) return fail;

		// 3. 인증번호를 서버에서 다시 확인 (화면에서 확인했어도 한 번 더 → 이때 '사용함' 처리)
		if (!verifyCode(dto.getEmail(), "RESET_PW", dto.getCode())) return fail;

		// 4. BCrypt 로 바꿔서 저장 (원래 비밀번호는 복원할 수 없음)
		dao.updatePassword(member.getMemberId(), encoder.encode(newPw));
		return null; // 성공
	}

	// ===== 이메일 변경 인증번호 보내기 (M05) =====
	// 새 이메일로 6자리를 보냄. 문제가 있으면 오류 문구를, 성공하면 null 을 돌려준다
	// (로그인한 본인만 쓰는 기능이라 오류를 그대로 알려 줘도 됨)
	public String sendEmailChangeCode(int memberId, String newEmail) {

		// 1. 형식 검사
		if (newEmail == null || !newEmail.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
			return "이메일 형식이 올바르지 않습니다.";
		}

		// 2. 지금 이메일과 같으면 인증할 필요 없음
		MemberDTO me = dao.selectById(memberId);
		if (me == null || "WITHDRAWN".equals(me.getStatus())) return "회원 정보를 찾을 수 없습니다.";
		if (newEmail.equals(me.getEmail())) return "지금 쓰는 이메일이에요.";

		// 3. 다른 회원이 쓰는 이메일이면 안 됨
		if (dao.countEmailExceptMe(memberId, newEmail) > 0) return "이미 사용 중인 이메일입니다.";

		// 4. 재발송은 1분 뒤부터
		if (dao.countRecentAuth(newEmail, "CHANGE_EMAIL") > 0) {
			return "인증번호는 1분 뒤에 다시 받을 수 있어요.";
		}

		// 5. 6자리 만들어 저장 (유효 5분) → 새 이메일로 발송
		String code = String.format("%06d", new SecureRandom().nextInt(1000000));
		dao.insertEmailAuth(newEmail, code, "CHANGE_EMAIL");
		try {
			sendCodeMail(newEmail, code, "CHANGE_EMAIL");
		} catch (Exception e) {
			e.printStackTrace();
			return "메일을 보내지 못했습니다. 이메일 주소를 확인해 주세요.";
		}
		return null; // 성공
	}

	// ===== 인증번호 메일 보내기 (M05·M09 같이 씀) =====
	// HTML 메일 (밤하늘 카드 + 금색 인증번호)
	private void sendCodeMail(String to, String code, String purpose) throws Exception {
		MimeMessage mail = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(mail, "UTF-8");
		helper.setFrom(mailFrom);
		helper.setTo(to);
		helper.setSubject("[환승인생] 인증번호 안내");
		helper.setText(makeMailHtml(code, purpose), true); // true = HTML 로 보내기
		mailSender.send(mail);
	}

	// ===== 인증번호 메일 HTML 만들기 (M09) =====
	// 메일 프로그램(Gmail·네이버)은 CSS 변수·<style>·flex 를 잘 못 읽어서
	// <table> + style="..." 로 만들고, 색도 design.css 값(#0a0f22, #f2d58c …)을 직접 씀
	private String makeMailHtml(String code, String purpose) {
		String title = switch (purpose) {
			case "FIND_ID" -> "아이디 찾기";
			case "RESET_PW" -> "비밀번호 찾기";
			default -> "이메일 변경"; // CHANGE_EMAIL
		};

		// 123456 → "1 2 3 4 5 6" 처럼 띄워서 읽기 쉽게
		String spaced = String.join(" ", code.split(""));

		return """
			<div style="margin:0; padding:32px 0; background:#070b1a;">
			  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
			    <tr><td align="center">
			      <table role="presentation" width="420" cellpadding="0" cellspacing="0"
			             style="background:#0f1530; border:1px solid #2b3459; border-radius:16px;
			                    font-family:'Malgun Gothic','Apple SD Gothic Neo',sans-serif;">
			        <tr><td style="padding:28px 32px 0; color:#f2d58c; font-size:18px; font-weight:700;">
			          &#9733; 환승인생
			        </td></tr>
			        <tr><td style="padding:20px 32px 0; color:#e8e9f5; font-size:20px; font-weight:700;">
			          %s 인증번호를 알려 드려요
			        </td></tr>
			        <tr><td style="padding:8px 32px 0; color:#a3a8cc; font-size:14px; line-height:1.6;">
			          아래 6자리를 화면에 입력해 주세요.
			        </td></tr>
			        <tr><td align="center" style="padding:24px 32px;">
			          <div style="display:inline-block; padding:16px 28px; background:#141b3a;
			                      border:1px solid #d9b25f; border-radius:12px;
			                      color:#f2d58c; font-size:32px; font-weight:700; letter-spacing:6px;">
			            %s
			          </div>
			        </td></tr>
			        <tr><td style="padding:0 32px 28px; color:#7d83ab; font-size:12px; line-height:1.7;">
			          이 번호는 <b style="color:#e8e9f5;">5분 동안</b>만 쓸 수 있어요.<br>
			          본인이 요청하지 않았다면 이 메일은 무시해 주세요.
			        </td></tr>
			      </table>
			    </td></tr>
			  </table>
			</div>
			""".formatted(title, spaced);
	}

	// ===== 내 특성 레벨 (C01, M08) =====
	public List<MemberTraitDTO> myTraits(int memberId) {
		return dao.selectMyTraits(memberId);
	}

	// ===== 전생 기록 (M07) =====
	// 끝난(ENDED) 인생 목록을 한 페이지에 10개씩 + 위쪽 숫자 칸(살아본 인생, 퀘스트 달성, 획득 포인트)
	public Map<String, Object> myLives(int memberId, int page, String sort) {
		int size = 10;
		if (page < 1) page = 1;
		if (!"age".equals(sort)) sort = "recent"; // 이상한 값이 오면 최신순

		// 1. 몇 번째 줄부터 몇 번째 줄까지 (1페이지: 1~10, 2페이지: 11~20 …)
		int start = (page - 1) * size; // 이 줄 "다음"부터
		int end = page * size;         // 이 줄까지

		// 2. DB 에서 가져오기
		List<LifeRecordDTO> lives = dao.selectMyLives(memberId, sort, start, end);
		int total = dao.countMyEndedLives(memberId);

		// 3. 화면에 보낼 상자 (Map 에 이름표 붙여 담기)
		return Map.of(
				"total", total,                           // 살아본 인생 수
				"page", page,
				"size", size,
				"questDone", dao.countMyQuestDone(memberId), // 퀘스트 달성 수
				"pointSum", dao.sumMyQuestPoint(memberId),   // 퀘스트로 받은 포인트 합계
				"lives", lives);
	}

	// ===== 특성 레벨 올리기 (M08) =====
	// 포인트 1점 차감 + 특성 1레벨 상승 + 포인트 내역 저장을 한 번에 (하나라도 실패하면 모두 취소)
	// 버튼을 빠르게 두 번 눌러도 SQL 의 조건(WHERE POINT >= 1, TRAIT_LEVEL < 5) 때문에 넘치지 않음
	@Transactional
	public void levelUp(int memberId, String traitId) {

		// 1. 특성 레벨 +1 (이미 최대 레벨이면 0줄이 바뀜)
		if (dao.increaseTraitLevel(memberId, traitId) == 0) {
			throw new IllegalStateException("이미 최대 레벨이거나 없는 특성이에요.");
		}

		// 2. 포인트 -1 (포인트가 0이면 0줄이 바뀜 → 예외 → 1번도 같이 취소)
		if (dao.decreasePoint(memberId) == 0) {
			throw new IllegalStateException("포인트가 부족해요.");
		}

		// 3. 포인트 내역 저장 (AMOUNT -1, REASON 'TRAIT_LEVELUP', 인생과는 상관없어서 LIFE_ID 는 NULL)
		dao.insertTraitLevelUpHistory(memberId);
	}
}
