package com.example.demo.member;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import jakarta.mail.internet.MimeMessage;

// 실제 일(검사, 암호화, 저장)을 하는 곳
@Service
public class MemberService {

	private MemberDAO dao;
	private JavaMailSender mailSender; // 메일 보내는 도구 (M09)
	private String mailFrom;           // 보내는 사람 = Gmail 주소 (spring.mail.username)
	private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

	// 카카오 로그인 설정 (application.properties)
	@Value("${kakao.client.id}")
	private String kakaoClientId;       // REST API 키
	@Value("${kakao.redirect.uri}")
	private String kakaoRedirectUri;    // 카카오가 code 를 붙여 돌려보낼 프론트 주소
	@Value("${kakao.client.secret:}")
	private String kakaoClientSecret;   // 카카오 콘솔에서 Client Secret 을 켰을 때만 (없으면 빈 값)

	// 회원가입 로봇 확인 (reCAPTCHA) 비밀 키 - application-secret.properties 의 recaptcha.secret
	@Value("${recaptcha.secret:}")
	private String recaptchaSecret;

	private RestClient restClient = RestClient.create(); // 카카오 서버에 요청 보내는 도구

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
			return "비밀번호 확인이 맞지 않아요.";
		}
		if (dto.getNickname() == null || dto.getNickname().length() < 2 || dto.getNickname().length() > 10) {
			return "닉네임은 2~10자로 입력해 주세요.";
		}
		if (dto.getEmail() == null || !dto.getEmail().matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
			return "이메일 형식이 맞지 않아요.";
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
		if (!isLoginIdAvailable(dto.getLoginId())) return "이미 사용 중인 아이디예요.";
		if (!isNicknameAvailable(dto.getNickname())) return "이미 사용 중인 닉네임이에요.";
		if (!isEmailAvailable(dto.getEmail())) return "이미 사용 중인 이메일이에요.";

		// 2-1. 로봇 확인 (다른 검사를 다 통과한 뒤에 구글에 물어봄)
		if (!verifyCaptcha(dto.getCaptchaToken())) {
			return "로봇이 아닌지 다시 확인해 주세요.";
		}

		// 3. 비밀번호를 BCrypt 로 바꿔서 담기 (원래 비밀번호는 저장 안 함)
		dto.setPassword(encoder.encode(dto.getPassword()));

		// 4. MEMBER 에 1줄 저장 → dto.memberId 에 새 번호가 들어옴
		dao.insertMember(dto);

		// 5. 그 번호로 특성 4종 Lv.1 저장
		dao.insertDefaultTraits(dto.getMemberId());

		return null; // 성공
	}

	// 로봇 확인: 화면에서 받은 토큰을 구글에 보내서 진짜인지 물어봄
	// POST https://www.google.com/recaptcha/api/siteverify (form: secret, response) → { success: true/false }
	private boolean verifyCaptcha(String token) {
		if (token == null || token.isBlank()) return false;

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("secret", recaptchaSecret);
		form.add("response", token);

		try {
			Map<?, ?> res = restClient.post()
					.uri("https://www.google.com/recaptcha/api/siteverify")
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(form)
					.retrieve()
					.body(Map.class);
			return res != null && Boolean.TRUE.equals(res.get("success"));
		} catch (Exception e) {
			// 구글 서버에 연결 못 함 → 통과시키지 않음
			return false;
		}
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

	// ===== 카카오 로그인 (M02 소셜) =====
	// 흐름: 프론트가 받은 code → ① 카카오에 토큰 요청 → ② 토큰으로 사용자 정보 요청
	//       → ③ 카카오 번호로 회원 찾기 (없으면 자동 가입) → 회원 정보 돌려주기
	// 실패하거나 탈퇴한 회원이면 null
	// DB 는 안 바꿈: LOGIN_ID = "kakao_카카오번호" 로 카카오 회원을 구분
	@Transactional // 자동 가입할 때 회원 INSERT 와 특성 INSERT 를 하나로 묶음
	public MemberDTO kakaoLogin(String code) {
		if (code == null || code.isBlank()) return null;

		// ① code → 카카오 액세스 토큰
		String accessToken = requestKakaoToken(code);
		if (accessToken == null) return null;

		// ② 토큰 → 카카오 사용자 정보 { id, kakao_account: { email, profile: { nickname } } }
		Map<String, Object> info = requestKakaoUser(accessToken);
		if (info == null || info.get("id") == null) return null;

		// ③ 카카오 번호로 회원 찾기
		String loginId = "kakao_" + info.get("id");
		MemberDTO member = dao.selectByLoginId(loginId);

		if (member != null) {
			// 이미 가입한 카카오 회원 → 탈퇴했으면 로그인 불가 (일반 로그인과 같은 규칙)
			return "WITHDRAWN".equals(member.getStatus()) ? null : member;
		}

		// ④ 처음 온 카카오 회원 → 자동 가입
		MemberDTO dto = new MemberDTO();
		dto.setLoginId(loginId);
		dto.setPassword(encoder.encode(UUID.randomUUID().toString())); // 아무도 모르는 비밀번호 (아이디·비번 로그인은 못 함)
		dto.setNickname(makeKakaoNickname(info));
		dto.setEmail(makeKakaoEmail(info, loginId));
		dto.setPhone(null);              // 카카오는 전화번호를 받지 않음
		dto.setPrivacyAgreed("Y");        // 카카오 동의 화면에서 동의함
		dao.insertMember(dto);            // memberId 가 채워짐 (selectKey)
		dao.insertDefaultTraits(dto.getMemberId()); // 특성 4종 Lv.1

		return dao.selectByLoginId(loginId); // 권한(USER) 등 DB 기본값까지 담아서
	}

	// 카카오 ① 토큰 받기  POST https://kauth.kakao.com/oauth/token (form 형식)
	private String requestKakaoToken(String code) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("client_id", kakaoClientId);
		form.add("redirect_uri", kakaoRedirectUri);
		form.add("code", code);
		if (!kakaoClientSecret.isBlank()) form.add("client_secret", kakaoClientSecret);

		try {
			Map<?, ?> res = restClient.post()
					.uri("https://kauth.kakao.com/oauth/token")
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(form)
					.retrieve()
					.body(Map.class);
			return res == null ? null : (String) res.get("access_token");
		} catch (Exception e) {
			// code 를 두 번 썼거나 키·주소가 틀린 경우 → null (컨트롤러가 401)
			return null;
		}
	}

	// 카카오 ② 사용자 정보  GET https://kapi.kakao.com/v2/user/me (Authorization: Bearer 토큰)
	@SuppressWarnings("unchecked")
	private Map<String, Object> requestKakaoUser(String accessToken) {
		try {
			return restClient.get()
					.uri("https://kapi.kakao.com/v2/user/me")
					.header("Authorization", "Bearer " + accessToken)
					.retrieve()
					.body(Map.class);
		} catch (Exception e) {
			// 카카오 토큰이 잘못됐거나 카카오 서버 오류 → null (컨트롤러가 401)
			return null;
		}
	}

	// 닉네임: 카카오 닉네임 (2~10자 규칙에 맞추기), 겹치면 뒤에 숫자 붙이기
	@SuppressWarnings("unchecked")
	private String makeKakaoNickname(Map<String, Object> info) {
		String name = "카카오회원";
		Map<String, Object> account = (Map<String, Object>) info.get("kakao_account");
		if (account != null && account.get("profile") != null) {
			Object nick = ((Map<String, Object>) account.get("profile")).get("nickname");
			if (nick != null && !nick.toString().isBlank()) name = nick.toString().trim();
		}
		if (name.length() < 2) name = name + "님";
		if (name.length() > 6) name = name.substring(0, 6); // 숫자 4자리 붙일 자리 남기기

		String nickname = name;
		while (dao.countByNickname(nickname) > 0) { // 이미 있으면 다시 뽑기
			nickname = name + (1000 + new SecureRandom().nextInt(9000));
		}
		return nickname;
	}

	// 이메일: 카카오 이메일 (동의했고 안 겹치면), 아니면 임시 주소 kakao_번호@kakao.local
	@SuppressWarnings("unchecked")
	private String makeKakaoEmail(Map<String, Object> info, String loginId) {
		Map<String, Object> account = (Map<String, Object>) info.get("kakao_account");
		if (account != null && account.get("email") != null) {
			String email = account.get("email").toString();
			if (dao.countByEmail(email) == 0) return email;
		}
		return loginId + "@kakao.local";
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
		if (me == null || "WITHDRAWN".equals(me.getStatus())) return "회원 정보를 찾을 수 없어요.";

		// 2. 현재 비밀번호 확인 (BCrypt 비교)
		if (dto.getCurrentPassword() == null || !encoder.matches(dto.getCurrentPassword(), me.getPassword())) {
			return "현재 비밀번호가 맞지 않아요.";
		}

		// 3. 닉네임·이메일 형식 검사 (회원가입과 같은 규칙)
		if (dto.getNickname() == null || dto.getNickname().length() < 2 || dto.getNickname().length() > 10) {
			return "닉네임은 2~10자로 입력해 주세요.";
		}
		if (dto.getEmail() == null || !dto.getEmail().matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
			return "이메일 형식이 맞지 않아요.";
		}
		// 전화번호 (회원가입과 같은 규칙, 숫자만 남겨서 저장)
		String phone = dto.getPhone() == null ? "" : dto.getPhone().replaceAll("[^0-9]", "");
		if (!phone.matches("^01[0-9]{8,9}$")) {
			return "전화번호를 올바르게 입력해 주세요. (예: 010-1234-5678)";
		}
		dto.setPhone(phone);

		// 4. 중복 검사 (나 자신은 빼고 셈 → 닉네임 그대로 두면 통과)
		if (dao.countNicknameExceptMe(memberId, dto.getNickname()) > 0) return "이미 사용 중인 닉네임이에요.";
		if (dao.countEmailExceptMe(memberId, dto.getEmail()) > 0) return "이미 사용 중인 이메일이에요.";

		// 5. 새 비밀번호: 비워 두면 안 바꿈(null), 입력했으면 검사 후 BCrypt 로 바꿔서 담기
		String newPw = dto.getNewPassword();
		if (newPw == null || newPw.isBlank()) {
			dto.setNewPassword(null);
		} else {
			if (!newPw.matches("^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9]).{8,20}$")) {
				return "새 비밀번호는 영문·숫자·특수문자를 포함해 8~20자로 입력해 주세요.";
			}
			if (!newPw.equals(dto.getNewPasswordCheck())) {
				return "새 비밀번호 확인이 맞지 않아요.";
			}
			dto.setNewPassword(encoder.encode(newPw));
		}

		// 6. 이메일을 바꿨으면 새 이메일로 받은 인증번호 확인 (안 바꿨으면 건너뜀)
		if (!dto.getEmail().equals(me.getEmail())) {
			if (!verifyCode(dto.getEmail(), "CHANGE_EMAIL", dto.getEmailCode())) {
				return "이메일 인증번호가 맞지 않거나 시간이 지났어요.";
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
			return "잘못된 요청이에요.";
		}
		if (dto.getEmail() == null || !dto.getEmail().matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
			return "이메일 형식이 맞지 않아요.";
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

	@Transactional
	public String findId(String email, String code) {
		// 1. 이메일로 회원 찾기 (먼저 → 없으면 인증번호를 쓰지 않음)
		MemberDTO member = dao.selectActiveByEmail(email);
		if (member == null) return null;

		// 2. 인증번호를 서버에서 다시 확인 (화면에서 확인했어도 한 번 더 → 이때 '사용함' 처리)
		if (!verifyCode(email, "FIND_ID", code)) return null;

		// 앞 3글자만 보여 주고 나머지는 *** (아이디는 4자 이상이라 안전)
		return member.getLoginId().substring(0, 3) + "***";
	}

	// ===== 아이디·비밀번호 찾기 - 인증번호 먼저 확인 (M09) =====
	// purpose: FIND_ID(아이디 찾기) 또는 RESET_PW(비밀번호 찾기)
	// 번호를 '사용함'으로 바꾸지 않음 → 아이디 확인(findId)·비밀번호 변경(resetPw) 때 한 번 더 확인하고 그때 사용 처리
	public String checkCode(MemberFindDTO dto) {
		String fail = "인증번호가 맞지 않거나 시간이 지났어요.";
		String purpose = dto.getPurpose();
		if (!"FIND_ID".equals(purpose) && !"RESET_PW".equals(purpose)) return "잘못된 요청이에요.";
		if (dto.getEmail() == null || dto.getCode() == null) return fail;

		// 1. 맞는 회원인지 먼저 (안 맞아도 똑같은 문구 → 가입 여부 안 드러남)
		//    아이디 찾기: 이메일만 / 비밀번호 찾기: 아이디 + 이메일
		MemberDTO member = "FIND_ID".equals(purpose)
				? dao.selectActiveByEmail(dto.getEmail())
				: dao.selectActiveByLoginIdAndEmail(dto.getLoginId(), dto.getEmail());

		// 2. 회원이 없거나 번호가 틀리면 틀린 횟수 +1 (5번 틀리면 그 번호는 못 씀)
		if (member == null || dao.countValidAuth(dto.getEmail(), purpose, dto.getCode()) == 0) {
			dao.increaseAuthFail(dto.getEmail(), purpose);
			return fail;
		}
		return null; // 인증 성공
	}

	// ===== 비밀번호 재설정 (M09) =====
	@Transactional // 번호를 '사용함'으로 바꾸는 것과 비밀번호 저장 중 하나라도 실패하면 둘 다 취소
	public String resetPassword(MemberFindDTO dto) {

		// 1. 새 비밀번호 검사 (먼저 검사해야 형식 실수로 인증번호가 헛되이 쓰이지 않음)
		String newPw = dto.getNewPassword();
		if (newPw == null || !newPw.matches("^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9]).{8,20}$")) {
			return "비밀번호는 영문·숫자·특수문자를 포함해 8~20자로 입력해 주세요.";
		}
		if (!newPw.equals(dto.getNewPasswordCheck())) {
			return "비밀번호 확인이 맞지 않아요.";
		}

		// 2. 아이디와 이메일이 맞는 회원 찾기 (먼저 → 안 맞으면 인증번호를 쓰지 않음)
		String fail = "인증번호가 맞지 않거나 시간이 지났어요.";
		MemberDTO member = dao.selectActiveByLoginIdAndEmail(dto.getLoginId(), dto.getEmail());
		if (member == null) return fail;

		// 3. 인증번호를 서버에서 다시 확인 (화면에서 확인했어도 한 번 더 → 이때 '사용함' 처리)
		if (!verifyCode(dto.getEmail(), "RESET_PW", dto.getCode())) return fail;

		// 4. BCrypt 로 바꿔서 저장 (원래 비밀번호는 복원할 수 없음)
		dao.updatePassword(member.getMemberId(), encoder.encode(newPw));
		return null; // 성공
	}

	// ===== 이메일 변경 인증번호 보내기 (M05) =====
	// (로그인한 본인만 쓰는 기능이라 오류를 그대로 알려 줘도 됨)
	public String sendEmailChangeCode(int memberId, String newEmail) {

		// 1. 형식 검사
		if (newEmail == null || !newEmail.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
			return "이메일 형식이 맞지 않아요.";
		}

		// 2. 지금 이메일과 같으면 인증할 필요 없음
		MemberDTO me = dao.selectById(memberId);
		if (me == null || "WITHDRAWN".equals(me.getStatus())) return "회원 정보를 찾을 수 없어요.";
		if (newEmail.equals(me.getEmail())) return "지금 쓰는 이메일이에요.";

		// 3. 다른 회원이 쓰는 이메일이면 안 됨
		if (dao.countEmailExceptMe(memberId, newEmail) > 0) return "이미 사용 중인 이메일이에요.";

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
			return "메일을 보내지 못했어요. 이메일 주소를 확인해 주세요.";
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
