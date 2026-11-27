package com.example.demo.member;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

import com.example.demo.chat.ChatDAO;

import jakarta.mail.internet.MimeMessage;

@Service
public class MemberService {

	// 인증 메일은 이 일꾼 2명이 차례로 보냄 (요청마다 새 스레드를 만들지 않게 → 한꺼번에 몰려도 서버가 버팀)
	private static final ExecutorService MAIL_WORKERS = Executors.newFixedThreadPool(2);

	// 로그인 무차별 대입 방지: 같은 아이디 15분 내 5회 연속 실패 시 5분간 잠금
	private static final int MAX_LOGIN_FAIL = 5;
	private static final long LOGIN_LOCK_MS = 5 * 60 * 1000L;
	private static final long LOGIN_FAIL_WINDOW_MS = 15 * 60 * 1000L;

	private static class LoginAttempt {
		int failCount;
		long lastFailAt;
		long lockedUntil;
	}

	private final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();

	private MemberDAO dao;
	private ChatDAO chatDAO;           // 금칙어 목록 (BANNED_WORD, 닉네임에도 금칙어 막기)
	private JavaMailSender mailSender; // 메일 보내는 도구 (M09)
	private String mailFrom;           // 보내는 사람 = Gmail 주소 (spring.mail.username)
	private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

	// 회원가입 로봇 확인 (reCAPTCHA) 비밀 키 - application-secret.properties 의 recaptcha.secret
	@Value("${recaptcha.secret:}")
	private String recaptchaSecret;

	// 구글 OAuth 클라이언트 ID
	@Value("${google.client.id:1060054612101-00gbenedhdoubrsnk69e4guoblbpdovc.apps.googleusercontent.com}")
	private String googleClientId;

	private RestClient restClient = RestClient.create(); // 구글(reCAPTCHA·OAuth)에 요청 보내는 도구

	public MemberService(MemberDAO dao, ChatDAO chatDAO, JavaMailSender mailSender,
			@Value("${spring.mail.username}") String mailFrom) {
		this.dao = dao;
		this.chatDAO = chatDAO;
		this.mailSender = mailSender;
		this.mailFrom = mailFrom;
	}

	// 금칙어 포함 여부 (띄어쓰기·대소문자 무시)
	public boolean isNicknameBanned(String text) {
		return hasBannedWord(text);
	}

	private boolean hasBannedWord(String text) {
		if (text == null) return false;
		String plain = text.replaceAll("\\s", "").toLowerCase();
		for (String word : chatDAO.selectBannedWords()) {
			String w = word.replaceAll("\\s", "").toLowerCase();
			if (!w.isEmpty() && plain.contains(w)) return true;
		}
		return false;
	}

	// ===== 중복 확인 (true = 사용 가능) =====
	public boolean isLoginIdAvailable(String loginId) {
		return dao.countByLoginId(loginId) == 0;
	}

	public boolean isNicknameAvailable(String nickname) {
		String clean = nickname == null ? null : nickname.trim();
		if (clean == null || hasBannedWord(clean)) return false;
		return dao.countByNickname(clean) == 0;
	}

	public boolean isEmailAvailable(String email) {
		return dao.countByEmail(cleanEmail(email)) == 0;
	}

	// 이메일 정리: 앞뒤 공백 빼고 소문자로 (" ABC@b.com" 과 "abc@b.com" 을 같은 이메일로)
	private String cleanEmail(String email) {
		return email == null ? null : email.trim().toLowerCase();
	}

	// ===== 회원가입 (M01) =====
	// 문제가 있으면 오류 문구를 돌려주고, 성공하면 null 을 돌려준다
	@Transactional // 회원 INSERT 와 특성 INSERT 중 하나라도 실패하면 둘 다 취소
	public String join(MemberDTO dto) {
		dto.setEmail(cleanEmail(dto.getEmail()));
		if (dto.getLoginId() != null) dto.setLoginId(dto.getLoginId().trim());
		if (dto.getNickname() != null) dto.setNickname(dto.getNickname().trim()); // "민승 " 과 "민승" 을 같은 닉네임으로

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
		if (hasBannedWord(dto.getNickname())) {
			return "닉네임에 쓸 수 없는 말이 들어 있어요.";
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

		return null;
	}

	// 로봇 확인: 화면에서 받은 토큰을 구글에 보내서 진짜인지 물어봄
	// POST https://www.google.com/recaptcha/api/siteverify (form: secret, response)
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

	// 구글 소셜 로그인 계정 여부 판별
	public boolean isGoogleAccount(MemberDTO member) {
		return member != null && member.getLoginId() != null && member.getLoginId().startsWith("g_");
	}

	public boolean isGoogleMemberId(int memberId) {
		return isGoogleAccount(dao.selectById(memberId));
	}

	// 로그인 잠금 여부 확인 (5회 연속 실패 시 5분간 잠금)
	public boolean isLoginLocked(String loginId) {
		if (loginId == null || loginId.isBlank()) return false;
		String key = loginId.trim().toLowerCase();
		LoginAttempt attempt = loginAttempts.get(key);
		if (attempt == null) return false;
		long now = System.currentTimeMillis();
		if (attempt.lockedUntil > now) return true;
		if (attempt.lockedUntil > 0) {
			loginAttempts.remove(key, attempt);
		}
		return false;
	}

	private void recordLoginFail(String key) {
		long now = System.currentTimeMillis();
		if (loginAttempts.size() > 1000) {
			loginAttempts.entrySet().removeIf(e -> {
				LoginAttempt v = e.getValue();
				return (v.lockedUntil > 0 && v.lockedUntil <= now)
						|| (v.lockedUntil == 0 && now - v.lastFailAt > LOGIN_FAIL_WINDOW_MS);
			});
		}
		loginAttempts.compute(key, (k, prev) -> {
			boolean expired = prev == null
					|| (prev.lockedUntil > 0 && prev.lockedUntil <= now)
					|| (prev.lockedUntil == 0 && now - prev.lastFailAt > LOGIN_FAIL_WINDOW_MS);
			LoginAttempt a = expired ? new LoginAttempt() : prev;
			a.failCount++;
			a.lastFailAt = now;
			if (a.failCount >= MAX_LOGIN_FAIL) {
				a.lockedUntil = now + LOGIN_LOCK_MS;
			}
			return a;
		});
	}

	// ===== 로그인 (M02) =====
	// 아이디·비밀번호가 맞으면 회원 정보를, 틀리면 null 을 돌려준다
	public MemberDTO login(String loginId, String password) {

		// 0. 빈 값이면 바로 실패
		if (loginId == null || password == null) return null;
		loginId = loginId.trim();
		if (loginId.isEmpty()) return null;

		String key = loginId.toLowerCase();
		if (isLoginLocked(loginId)) return null;

		// 1. 아이디로 회원 찾기
		MemberDTO member = dao.selectByLoginId(loginId);
		if (member == null) {
			recordLoginFail(key);
			return null; // 없는 아이디
		}

		// 2. 탈퇴한 회원은 로그인 불가
		if ("WITHDRAWN".equals(member.getStatus())) {
			recordLoginFail(key);
			return null;
		}

		// 3. 입력한 비밀번호를 DB의 BCrypt 값과 비교
		if (!encoder.matches(password, member.getPassword())) {
			recordLoginFail(key);
			return null;
		}

		loginAttempts.remove(key);
		return member;
	}

	// ===== 구글 소셜 로그인 · 가입 (M01, M02) =====

	// 이메일로 활성 회원 찾기 (구글 로그인 시 기존 회원 여부 확인)
	public MemberDTO findActiveByEmail(String email) {
		if (email == null || email.isBlank()) return null;
		return dao.selectActiveByEmail(cleanEmail(email));
	}

	// 구글 토큰(ID 토큰 또는 Access 토큰)을 구글 서버에 보내서 진짜인지 검증하고 이메일·고유번호·추천 닉네임 꺼내기
	public Map<String, Object> verifyGoogleToken(String credential) {
		if (credential == null || credential.isBlank()) return null;
		try {
			Map<?, ?> res;
			// 1) 점(.) 2개가 있는 JWT 형태면 id_token 검증
			if (credential.split("\\.").length == 3) {
				res = restClient.get()
						.uri("https://oauth2.googleapis.com/tokeninfo?id_token={token}", credential)
						.retrieve()
						.body(Map.class);
				if (res == null) return null;
				String aud = String.valueOf(res.get("aud"));
				if (!googleClientId.equals(aud)) return null;
			} else {
				// 2) Access Token 형태면 tokeninfo로 우리 클라이언트 ID 확인 후 userinfo에서 프로필 조회
				Map<?, ?> tokenInfo = restClient.get()
						.uri("https://oauth2.googleapis.com/tokeninfo?access_token={token}", credential)
						.retrieve()
						.body(Map.class);
				if (tokenInfo == null) return null;
				String aud = String.valueOf(tokenInfo.get("aud"));
				String azp = String.valueOf(tokenInfo.get("azp"));
				if (!googleClientId.equals(aud) && !googleClientId.equals(azp)) return null;

				res = restClient.get()
						.uri("https://www.googleapis.com/oauth2/v3/userinfo")
						.header("Authorization", "Bearer " + credential)
						.retrieve()
						.body(Map.class);
				if (res == null) return null;
			}

			// 이메일 인증이 완료된 구글 계정인지 확인
			String emailVerified = String.valueOf(res.get("email_verified"));
			if (!"true".equalsIgnoreCase(emailVerified)) return null;

			String email = cleanEmail((String) res.get("email"));
			String sub = String.valueOf(res.get("sub"));
			String name = res.get("name") != null ? String.valueOf(res.get("name")).replaceAll("\\s+", "") : "";
			if (name.length() > 10) {
				name = name.substring(0, 10);
			}
			// 추천 닉네임에 금칙어가 들어있거나 2자 미만이면 빈칸으로
			if (name.length() < 2 || hasBannedWord(name)) {
				name = "";
			}

			return Map.of(
					"email", email,
					"sub", sub,
					"suggestedNickname", name);
		} catch (Exception e) {
			return null;
		}
	}

	// 구글 신규 회원 추가 정보(닉네임·전화번호·개인정보 동의) 검증 후 회원 등록
	@Transactional
	public Map<String, Object> googleJoin(String credential, String nickname, String phoneInput, String privacyAgreed) {
		Map<String, Object> gInfo = verifyGoogleToken(credential);
		if (gInfo == null) {
			return Map.of("error", "구글 인증이 만료되었거나 올바르지 않아요. 다시 시도해 주세요.");
		}

		String email = (String) gInfo.get("email");
		String sub = (String) gInfo.get("sub");
		String cleanNick = nickname == null ? "" : nickname.trim();

		// 1. 닉네임·전화번호·약관동의 검사 (일반 회원가입과 동일한 기준)
		if (cleanNick.length() < 2 || cleanNick.length() > 10) {
			return Map.of("error", "닉네임은 2~10자로 입력해 주세요.");
		}
		if (hasBannedWord(cleanNick)) {
			return Map.of("error", "닉네임에 쓸 수 없는 말이 들어 있어요.");
		}
		if (!isNicknameAvailable(cleanNick)) {
			return Map.of("error", "이미 사용 중인 닉네임이에요.");
		}
		String phone = phoneInput == null ? "" : phoneInput.replaceAll("[^0-9]", "");
		if (!phone.matches("^01[0-9]{8,9}$")) {
			return Map.of("error", "전화번호를 올바르게 입력해 주세요. (예: 010-1234-5678)");
		}
		if (!"Y".equals(privacyAgreed)) {
			return Map.of("error", "개인정보 수집·이용에 동의해 주세요.");
		}
		if (!isEmailAvailable(email)) {
			return Map.of("error", "이미 가입된 이메일이에요.");
		}

		// 2. 구글 고유 ID(sub)로 중복 없는 로그인 아이디 생성 (g_ + 최대 18자리)
		String baseSub = sub.replaceAll("[^0-9a-zA-Z]", "");
		if (baseSub.length() > 18) baseSub = baseSub.substring(0, 18);
		String loginId = "g_" + baseSub;
		int suffix = 1;
		while (!isLoginIdAvailable(loginId)) {
			loginId = "g_" + baseSub.substring(0, Math.min(15, baseSub.length())) + "_" + suffix++;
		}

		// 3. 비밀번호는 랜덤 암호화 값으로 설정 (일반 아이디/비밀번호 로그인 차단)
		String randomPw = encoder.encode(java.util.UUID.randomUUID().toString());

		MemberDTO dto = new MemberDTO();
		dto.setLoginId(loginId);
		dto.setPassword(randomPw);
		dto.setNickname(cleanNick);
		dto.setEmail(email);
		dto.setPhone(phone);
		dto.setPrivacyAgreed("Y");

		// 4. 회원 저장 + 기본 특성 4종(Lv.1) 저장
		dao.insertMember(dto);
		dao.insertDefaultTraits(dto.getMemberId());

		MemberDTO created = dao.selectByLoginId(loginId);
		return Map.of("member", created);
	}

	// ===== 비회원 인생 → 회원 인생 전환 (M02) =====
	// 전환했으면 true, 안 했으면 false
	public boolean convertGuestLife(int memberId, String guestToken) {

		// 1. 비회원으로 하던 인생이 없으면 할 일 없음
		if (guestToken == null || guestToken.isBlank()) return false;

		// 2. 회원에게 이미 진행 중인 인생이 있으면 전환하지 않음
		//    (회원당 진행 중 인생 1개 규칙, 남은 비회원 인생은 엔진 C03 이 7일 미접속 시 정리)
		if (dao.countPlayingLife(memberId) > 0) return false;

		// 3. 비회원 인생에 회원 번호 채우기
		return dao.convertGuestLife(memberId, guestToken) > 0;
	}

	// 로그인했는데 회원 인생이 이미 있어서 전환을 못 했을 때: 이 브라우저에 비회원 인생이 남아 있는지
	// → 있으면 화면이 "어느 인생을 이어갈까요?" 를 물어봄
	public boolean hasGuestLife(String guestToken) {
		if (guestToken == null || guestToken.isBlank()) return false;
		return dao.countGuestPlayingLife(guestToken) > 0;
	}

	// "비회원 인생 이어가기"를 골랐을 때: 회원이 하던 인생은 포기하고, 비회원 인생을 회원 인생으로 전환
	// (진행 중 인생 1개 규칙 때문에 포기 → 전환 순서, 둘 중 하나라도 실패하면 둘 다 취소)
	@Transactional
	public boolean takeGuestLife(int memberId, String guestToken) {
		if (!hasGuestLife(guestToken)) return false;
		dao.abandonPlayingLife(memberId);
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
		dto.setEmail(cleanEmail(dto.getEmail()));
		if (dto.getNickname() != null) dto.setNickname(dto.getNickname().trim());

		// 1. DB 에서 지금 내 정보 꺼내기 (비밀번호 비교용)
		MemberDTO me = dao.selectById(memberId);
		if (me == null || "WITHDRAWN".equals(me.getStatus())) return "회원 정보를 찾을 수 없어요.";

		boolean isGoogle = me.getLoginId() != null && me.getLoginId().startsWith("g_");

		// 2. 현재 비밀번호 확인 (구글 소셜 로그인 계정은 비밀번호가 없으므로 건너뜀)
		if (!isGoogle) {
			if (dto.getCurrentPassword() == null || !encoder.matches(dto.getCurrentPassword(), me.getPassword())) {
				return "현재 비밀번호가 맞지 않아요.";
			}
		} else {
			// 구글 연동 계정은 구글 이메일을 그대로 유지하고 비밀번호 변경은 하지 않음
			dto.setEmail(cleanEmail(me.getEmail()));
			dto.setNewPassword(null);
		}

		// 3. 닉네임·이메일 형식 검사 (회원가입과 같은 규칙)
		if (dto.getNickname() == null || dto.getNickname().length() < 2 || dto.getNickname().length() > 10) {
			return "닉네임은 2~10자로 입력해 주세요.";
		}
		if (hasBannedWord(dto.getNickname())) {
			return "닉네임에 쓸 수 없는 말이 들어 있어요.";
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
		if (!dto.getEmail().equalsIgnoreCase(me.getEmail())) {
			if (!verifyCode(dto.getEmail(), "CHANGE_EMAIL", dto.getEmailCode())) {
				return "이메일 인증번호가 맞지 않거나 시간이 지났어요.";
			}
		}

		// 7. 저장 (아이디는 바꾸지 않음)
		dto.setMemberId(memberId);
		dao.updateMember(dto);
		return null;
	}

	// ===== 회원 탈퇴 (M06) =====
	// 일반 회원: 비밀번호 확인 / 구글 회원: Google 토큰 재인증 후 연동 이메일 일치 확인
	// (회원 줄은 지우지 않음 → 인생·추모관 글과 외래키 오류 방지)
	@Transactional // 탈퇴 + 진행 중 인생 포기를 한 묶음으로
	public boolean withdraw(int memberId, String password, String googleCredential) {
		MemberDTO me = dao.selectById(memberId);
		if (me == null || "WITHDRAWN".equals(me.getStatus())) return false;
		if (isGoogleAccount(me)) {
			Map<String, Object> gInfo = verifyGoogleToken(googleCredential);
			if (gInfo == null) return false;
			String verifiedEmail = cleanEmail((String) gInfo.get("email"));
			if (verifiedEmail == null || !verifiedEmail.equals(cleanEmail(me.getEmail()))) return false;
		} else {
			if (password == null || !encoder.matches(password, me.getPassword())) return false;
		}

		if (dao.updateWithdrawn(memberId) == 0) return false;
		// 하던 인생이 '진행 중'으로 영원히 남지 않게 포기 처리 (끝난 인생 · 추모관은 그대로)
		dao.abandonPlayingLife(memberId);
		return true;
	}

	// ===== 인증번호 보내기 (M09) =====
	// 문제가 있으면 오류 문구를, 성공하면 null 을 돌려준다
	// ※ 가입된 이메일이든 아니든 겉으로는 똑같이 "성공"처럼 보이게 함 (가입 여부를 알 수 없게)
	public String sendCode(MemberFindDTO dto) {
		dto.setEmail(cleanEmail(dto.getEmail()));
		if (dto.getLoginId() != null) dto.setLoginId(dto.getLoginId().trim());

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
		if ("RESET_PW".equals(purpose) && dto.getLoginId().startsWith("g_")) {
			return "Google 소셜 로그인 계정은 비밀번호 찾기를 이용할 수 없어요. Google 로그인을 이용해 주세요.";
		}

		// 2. 재발송은 1분 뒤부터 (EMAIL_AUTH 에 1분 안에 만든 번호가 있으면 막기)
		if (dao.countRecentAuth(dto.getEmail(), purpose) > 0) {
			return "인증번호는 1분 뒤에 다시 받을 수 있어요.";
		}

		// 3. 6자리 랜덤 숫자 만들기 (000123 처럼 앞이 0이어도 6자리)
		String code = String.format("%06d", new SecureRandom().nextInt(1000000));

		// 4. DB 에 저장 (유효시간 5분)
		//    회원이 없어도 저장함 → 1분 제한이 똑같이 걸려서 "가입된 이메일인지" 알 수 없음
		dao.deleteOldAuth();
		dao.insertEmailAuth(dto.getEmail(), code, purpose);

		// 5. 일치하는 일반 회원이 있을 때만 메일 발송 (구글 소셜 계정은 찾기 대상 제외)
		MemberDTO member = "FIND_ID".equals(purpose)
				? dao.selectActiveByEmail(dto.getEmail())
				: dao.selectActiveByLoginIdAndEmail(dto.getLoginId(), dto.getEmail());

		// Gmail 보내기는 몇 초 걸려서, 메일은 일꾼(MAIL_WORKERS)에게 맡기고 화면에는 바로 대답함
		if (member != null && !isGoogleAccount(member)) {
			String email = dto.getEmail();
			MAIL_WORKERS.execute(() -> {
				try {
					sendCodeMail(email, code, purpose);
				} catch (Exception e) {
					// 메일이 안 나가도 화면에는 똑같은 문구를 보여 줌 (가입 여부 노출 방지)
					e.printStackTrace();
				}
			});
		}
		return null;
	}

	// 인증번호 정리: 메일에서 "1 2 3 4 5 6" 을 복사·붙여넣기 해도 공백을 제거해 비교
	private String cleanCode(String code) {
		return code == null ? null : code.replaceAll("\\s+", "");
	}

	// ===== 인증번호 확인 (아이디 찾기·비밀번호 재설정이 같이 씀) =====
	// 맞으면 true (그 번호는 '사용함'으로 바꿈), 틀리면 false (틀린 횟수 +1)
	private boolean verifyCode(String email, String purpose, String code) {
		code = cleanCode(code);
		if (email == null || email.isEmpty() || code == null || code.isEmpty()) return false;

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
		email = cleanEmail(email);
		code = cleanCode(code);
		if (email == null || email.isEmpty() || code == null || code.isEmpty()) return null;

		// 1. 이메일로 일반 회원 찾기 (먼저 → 없거나 구글 계정이면 인증번호를 쓰지 않음)
		MemberDTO member = dao.selectActiveByEmail(email);
		if (member == null || isGoogleAccount(member)) return null;

		// 2. 인증번호를 서버에서 다시 확인 (화면에서 확인했어도 한 번 더 → 이때 '사용함' 처리)
		if (!verifyCode(email, "FIND_ID", code)) return null;

		// 앞 3글자만 보여 주고 나머지는 ***
		String loginId = member.getLoginId();
		return loginId.substring(0, Math.min(3, loginId.length())) + "***";
	}

	// ===== 아이디·비밀번호 찾기 - 인증번호 먼저 확인 (M09) =====
	// purpose: FIND_ID(아이디 찾기) 또는 RESET_PW(비밀번호 찾기)
	// 번호를 '사용함'으로 바꾸지 않음 → 아이디 확인(findId)·비밀번호 변경(resetPw) 때 한 번 더 확인하고 그때 사용 처리
	public String checkCode(MemberFindDTO dto) {
		dto.setEmail(cleanEmail(dto.getEmail()));
		if (dto.getLoginId() != null) dto.setLoginId(dto.getLoginId().trim());
		String code = cleanCode(dto.getCode());
		String fail = "인증번호가 맞지 않거나 시간이 지났어요.";
		String purpose = dto.getPurpose();
		if (!"FIND_ID".equals(purpose) && !"RESET_PW".equals(purpose)) return "잘못된 요청이에요.";
		if (dto.getEmail() == null || dto.getEmail().isEmpty() || code == null || code.isEmpty()) return fail;
		if ("RESET_PW".equals(purpose) && (dto.getLoginId() == null || dto.getLoginId().isEmpty())) return fail;

		// 1. 맞는 일반 회원인지 먼저 (안 맞아도 똑같은 문구 → 가입 여부 안 드러남)
		//    아이디 찾기: 이메일만 / 비밀번호 찾기: 아이디 + 이메일
		MemberDTO member = "FIND_ID".equals(purpose)
				? dao.selectActiveByEmail(dto.getEmail())
				: dao.selectActiveByLoginIdAndEmail(dto.getLoginId(), dto.getEmail());

		// 2. 회원이 없거나 구글 계정이면 틀린 횟수를 올리지 않고 실패 (남이 다른 사람 번호를 망가뜨리지 못하게)
		if (member == null || isGoogleAccount(member)) return fail;

		// 3. 번호가 틀리면 틀린 횟수 +1 (5번 틀리면 그 번호는 못 씀)
		if (dao.countValidAuth(dto.getEmail(), purpose, code) == 0) {
			dao.increaseAuthFail(dto.getEmail(), purpose);
			return fail;
		}
		return null; // 인증 성공
	}

	// ===== 비밀번호 재설정 (M09) =====
	@Transactional // 번호를 '사용함'으로 바꾸는 것과 비밀번호 저장 중 하나라도 실패하면 둘 다 취소
	public String resetPassword(MemberFindDTO dto) {
		dto.setEmail(cleanEmail(dto.getEmail()));
		if (dto.getLoginId() != null) dto.setLoginId(dto.getLoginId().trim());
		String code = cleanCode(dto.getCode());

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
		if (dto.getLoginId() == null || dto.getLoginId().isEmpty()
				|| dto.getEmail() == null || dto.getEmail().isEmpty()) {
			return fail;
		}
		MemberDTO member = dao.selectActiveByLoginIdAndEmail(dto.getLoginId(), dto.getEmail());
		if (member == null) return fail;
		if (isGoogleAccount(member)) return "Google 소셜 로그인 계정은 비밀번호를 재설정할 수 없어요.";

		// 3. 인증번호를 서버에서 다시 확인 (화면에서 확인했어도 한 번 더 → 이때 '사용함' 처리)
		if (!verifyCode(dto.getEmail(), "RESET_PW", code)) return fail;

		// 4. BCrypt 로 바꿔서 저장 + 기존 로그인 5회 실패 잠금 해제
		dao.updatePassword(member.getMemberId(), encoder.encode(newPw));
		loginAttempts.remove(member.getLoginId().toLowerCase());
		return null;
	}

	// ===== 이메일 변경 인증번호 보내기 (M05) =====
	// (로그인한 본인만 쓰는 기능이라 오류를 그대로 알려 줘도 됨)
	public String sendEmailChangeCode(int memberId, String newEmail) {
		newEmail = cleanEmail(newEmail);

		// 1. 형식 검사
		if (newEmail == null || !newEmail.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
			return "이메일 형식이 맞지 않아요.";
		}

		// 2. 지금 이메일과 같으면 인증할 필요 없음
		MemberDTO me = dao.selectById(memberId);
		if (me == null || "WITHDRAWN".equals(me.getStatus())) return "회원 정보를 찾을 수 없어요.";
		if (newEmail.equalsIgnoreCase(me.getEmail())) return "지금 쓰는 이메일이에요.";

		// 3. 다른 회원이 쓰는 이메일이면 안 됨
		if (dao.countEmailExceptMe(memberId, newEmail) > 0) return "이미 사용 중인 이메일이에요.";

		// 4. 재발송은 1분 뒤부터
		if (dao.countRecentAuth(newEmail, "CHANGE_EMAIL") > 0) {
			return "인증번호는 1분 뒤에 다시 받을 수 있어요.";
		}

		// 5. 6자리 만들어 저장 (유효 5분) → 새 이메일로 발송
		String code = String.format("%06d", new SecureRandom().nextInt(1000000));
		dao.deleteOldAuth();
		dao.insertEmailAuth(newEmail, code, "CHANGE_EMAIL");
		// 로그인한 본인 기능이라 실패를 알려 줘도 됨 → 다 보낼 때까지 기다림 (화면은 그동안 입력칸을 먼저 열어 둠)
		try {
			sendCodeMail(newEmail, code, "CHANGE_EMAIL");
		} catch (Exception e) {
			e.printStackTrace();
			return "메일을 보내지 못했어요. 이메일 주소를 확인하고 1분 뒤 다시 받아 주세요.";
		}
		return null;
	}

	// ===== 인증번호 메일 보내기 (M05·M09 같이 씀) =====
	// HTML 메일
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
	// 버튼을 빠르게 두 번 눌러도 SQL 의 조건(WHERE POINT >= 1, TRAIT_LEVEL < 최대 레벨) 때문에 넘치지 않음
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
