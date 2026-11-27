package com.example.demo.member;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.commons.JwtUtil;

@RestController
@RequestMapping("/api/members")
public class MemberController {

	private MemberService service;
	private JwtUtil jwtUtil;

	public MemberController(MemberService service, JwtUtil jwtUtil) {
		this.service = service;
		this.jwtUtil = jwtUtil;
	}

	// 회원가입 (M01)  POST /api/members
	@PostMapping
	public ResponseEntity<Map<String, Object>> join(@RequestBody MemberDTO dto) {
		try {
			String error = service.join(dto);
			if (error != null) {
				return ResponseEntity.badRequest().body(Map.of("message", error));
			}
			return ResponseEntity.status(201).body(Map.of("message", "회원가입을 마쳤어요."));
		} catch (DuplicateKeyException e) {
			// 거의 동시에 같은 아이디로 가입한 경우 → DB UNIQUE 제약이 막아 줌
			return ResponseEntity.status(409).body(Map.of("message", "이미 사용 중인 정보가 있어요."));
		}
	}

	// 아이디 중복 확인  GET /api/members/checkId?loginId=abc
	@GetMapping("/checkId")
	public Map<String, Boolean> checkId(@RequestParam String loginId) {
		return Map.of("available", service.isLoginIdAvailable(loginId));
	}

	// 닉네임 중복 확인  GET /api/members/checkNickname?nickname=홍길동
	@GetMapping("/checkNickname")
	public Map<String, Object> checkNickname(@RequestParam String nickname) {
		if (service.isNicknameBanned(nickname)) {
			return Map.of("available", false, "message", "닉네임에 쓸 수 없는 말이 들어 있어요.");
		}
		boolean ok = service.isNicknameAvailable(nickname);
		return ok ? Map.of("available", true)
				: Map.of("available", false, "message", "이미 사용 중인 닉네임이에요.");
	}

	// 이메일 중복 확인  GET /api/members/checkEmail?email=a@b.com
	@GetMapping("/checkEmail")
	public Map<String, Boolean> checkEmail(@RequestParam String email) {
		return Map.of("available", service.isEmailAvailable(email));
	}

	// 로그인 (M02)  POST /api/members/login
	@PostMapping("/login")
	public ResponseEntity<Map<String, Object>> login(@RequestBody MemberLoginDTO dto) {
		if (service.isLoginLocked(dto.getLoginId())) {
			return ResponseEntity.status(429)
					.body(Map.of("message", "로그인 시도가 너무 많아요. 5분 뒤에 다시 시도해 주세요."));
		}

		MemberDTO member = service.login(dto.getLoginId(), dto.getPassword());

		if (member == null) {
			if (service.isLoginLocked(dto.getLoginId())) {
				return ResponseEntity.status(429)
						.body(Map.of("message", "로그인을 5번 틀려 5분간 잠겼어요. 잠시 후 다시 시도해 주세요."));
			}
			return ResponseEntity.status(401)
					.body(Map.of("message", "아이디 또는 비밀번호가 맞지 않아요."));
		}

		// 이 브라우저에서 비회원으로 하던 인생이 있으면 회원 인생으로 전환
		boolean converted = service.convertGuestLife(member.getMemberId(), dto.getGuestToken());
		
		String token = jwtUtil.createToken(String.valueOf(member.getMemberId()), member.getMemberRole(),
				JwtUtil.passwordVersion(member.getPassword()));

		// 전환 못 했는데 비회원 인생이 남아 있음 = 회원에게 이미 진행 중인 인생이 있음 → 화면이 어느 쪽을 할지 물어봄
		boolean guestPending = !converted && service.hasGuestLife(dto.getGuestToken());

		return ResponseEntity.ok(Map.of(
				"token", token,
				"nickname", member.getNickname(),
				"memberRole", member.getMemberRole(),
				"guestConverted", converted,
				"guestPending", guestPending));
	}

	// 구글 소셜 로그인 1단계: 구글 토큰으로 기존 회원 확인 (M02)  POST /api/members/google/login
	// - 이미 가입된 이메일이면 바로 JWT 토큰 발급 (needJoin: false)
	// - 처음 온 구글 계정이면 추가 정보(닉네임·전화번호·약관동의) 입력 요청 (needJoin: true)
	@PostMapping("/google/login")
	public ResponseEntity<Map<String, Object>> googleLogin(@RequestBody Map<String, String> body) {
		String credential = body.get("credential");
		String guestToken = body.get("guestToken");

		Map<String, Object> gInfo = service.verifyGoogleToken(credential);
		if (gInfo == null) {
			return ResponseEntity.status(401)
					.body(Map.of("message", "구글 인증에 실패했어요. 다시 시도해 주세요."));
		}

		String email = (String) gInfo.get("email");
		MemberDTO member = service.findActiveByEmail(email);

		// 아직 가입되지 않은 구글 이메일 → 추가 정보(닉네임·전화번호·동의) 입력 화면으로
		if (member == null) {
			if (!service.isEmailAvailable(email)) {
				return ResponseEntity.status(409)
						.body(Map.of("message", "사용할 수 없는 이메일이에요."));
			}
			return ResponseEntity.ok(Map.of(
					"needJoin", true,
					"email", email,
					"suggestedNickname", gInfo.getOrDefault("suggestedNickname", "")));
		}

		// 일반 회원가입으로 이미 등록된 이메일이면 구글 로그인 자동 연결 차단 (선가입 계정 탈취 방지)
		if (!service.isGoogleAccount(member)) {
			return ResponseEntity.status(409)
					.body(Map.of("message", "이미 일반 회원가입으로 등록된 이메일이에요. 아이디와 비밀번호로 로그인해 주세요."));
		}

		// 이미 가입된 구글 회원 → 일반 로그인과 동일하게 비회원 인생 전환 + JWT 발급
		boolean converted = service.convertGuestLife(member.getMemberId(), guestToken);
		String token = jwtUtil.createToken(String.valueOf(member.getMemberId()), member.getMemberRole(),
				JwtUtil.passwordVersion(member.getPassword()));
		boolean guestPending = !converted && service.hasGuestLife(guestToken);

		return ResponseEntity.ok(Map.of(
				"needJoin", false,
				"token", token,
				"nickname", member.getNickname(),
				"memberRole", member.getMemberRole(),
				"guestConverted", converted,
				"guestPending", guestPending));
	}

	// 구글 소셜 로그인 2단계: 신규 구글 회원 추가 정보(닉네임·전화번호·동의) 입력 후 가입 + 즉시 로그인
	// POST /api/members/google/join
	@PostMapping("/google/join")
	public ResponseEntity<Map<String, Object>> googleJoin(@RequestBody Map<String, String> body) {
		try {
			String credential = body.get("credential");
			String nickname = body.get("nickname");
			String phone = body.get("phone");
			String privacyAgreed = body.get("privacyAgreed");
			String guestToken = body.get("guestToken");

			Map<String, Object> result = service.googleJoin(credential, nickname, phone, privacyAgreed);
			if (result.containsKey("error")) {
				return ResponseEntity.badRequest().body(Map.of("message", result.get("error")));
			}

			MemberDTO member = (MemberDTO) result.get("member");
			boolean converted = service.convertGuestLife(member.getMemberId(), guestToken);
			String token = jwtUtil.createToken(String.valueOf(member.getMemberId()), member.getMemberRole(),
					JwtUtil.passwordVersion(member.getPassword()));
			boolean guestPending = !converted && service.hasGuestLife(guestToken);

			return ResponseEntity.ok(Map.of(
					"token", token,
					"nickname", member.getNickname(),
					"memberRole", member.getMemberRole(),
					"guestConverted", converted,
					"guestPending", guestPending));
		} catch (DuplicateKeyException e) {
			return ResponseEntity.status(409).body(Map.of("message", "이미 사용 중인 닉네임 또는 이메일이에요."));
		}
	}

	// 로그인 뒤 "비회원 인생 이어가기" (M02)  POST /api/members/me/takeGuestLife  { guestToken }
	// 회원이 하던 인생은 포기하고 비회원 인생을 회원 인생으로 옮김
	@PostMapping("/me/takeGuestLife")
	public ResponseEntity<Map<String, Object>> takeGuestLife(@RequestAttribute("memberId") String memberId,
			@RequestBody MemberLoginDTO dto) {
		if (!service.takeGuestLife(Integer.parseInt(memberId), dto.getGuestToken())) {
			return ResponseEntity.badRequest().body(Map.of("message", "비회원으로 하던 인생을 찾지 못했어요."));
		}
		return ResponseEntity.ok(Map.of("message", "비회원으로 하던 인생을 이어서 해요."));
	}

	// 마이페이지 (M04)  GET /api/members/me
	@GetMapping("/me")
	public ResponseEntity<MyPageDTO> myPage(@RequestAttribute("memberId") String memberId) {
		MyPageDTO my = service.myPage(Integer.parseInt(memberId));
		if (my == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(my);
	}

	// 개인정보 수정 (M05)  PUT /api/members/me
	@PutMapping("/me")
	public ResponseEntity<Map<String, Object>> update(@RequestAttribute("memberId") String memberId, @RequestBody MemberUpdateDTO dto) {
		try {
			String error = service.update(Integer.parseInt(memberId), dto);
			if (error != null) {
				return ResponseEntity.badRequest().body(Map.of("message", error));
			}
			
			return ResponseEntity.ok(Map.of("message", "저장했어요.", "nickname", dto.getNickname()));
		} catch (DuplicateKeyException e) {
			// 거의 동시에 같은 닉네임·이메일로 바꾼 경우 → DB UNIQUE 제약이 막아 줌
			return ResponseEntity.status(409).body(Map.of("message", "이미 사용 중인 닉네임 또는 이메일이에요."));
		}
	}

	// 이메일 변경 인증번호 보내기 (M05)  POST /api/members/me/emailCode
	@PostMapping("/me/emailCode")
	public ResponseEntity<Map<String, Object>> emailCode(@RequestAttribute("memberId") String memberId,
			@RequestBody MemberUpdateDTO dto) {
		String error = service.sendEmailChangeCode(Integer.parseInt(memberId), dto.getEmail());
		if (error != null) {
			return ResponseEntity.badRequest().body(Map.of("message", error));
		}
		return ResponseEntity.ok(Map.of("message", "새 이메일로 인증번호를 보냈어요. 5분 안에 입력해 주세요."));
	}

	// 회원 탈퇴 (M06)  POST /api/members/me/withdraw
	@PostMapping("/me/withdraw")
	public ResponseEntity<Map<String, Object>> withdraw(@RequestAttribute("memberId") String memberId,
			@RequestBody MemberUpdateDTO dto) {
		int id = Integer.parseInt(memberId);
		boolean ok = service.withdraw(id, dto.getCurrentPassword(), dto.getGoogleCredential());
		if (!ok) {
			String msg = service.isGoogleMemberId(id)
					? "Google 본인 확인에 실패했어요. 연동된 계정으로 다시 인증해 주세요."
					: "비밀번호가 맞지 않아요.";
			return ResponseEntity.badRequest().body(Map.of("message", msg));
		}
		return ResponseEntity.ok(Map.of("message", "탈퇴했어요."));
	}
	

	// 아이디·비밀번호 찾기 - 인증번호 보내기 (M09)  POST /api/members/find/sendCode
	@PostMapping("/find/sendCode")
	public ResponseEntity<Map<String, Object>> sendCode(@RequestBody MemberFindDTO dto) {
		String error = service.sendCode(dto);
		if(error != null) {
			return ResponseEntity.badRequest().body(Map.of("message",error));
		}
		return ResponseEntity.ok(Map.of("message", "입력한 정보가 맞다면 인증번호 메일을 보내 드려요."));
	}

	// 아이디 찾기 - 아이디 확인 (M09)  POST /api/members/find/id
	@PostMapping("/find/id")
	public ResponseEntity<Map<String, Object>> findId(@RequestBody MemberFindDTO dto) {
		String maskedId = service.findId(dto.getEmail(), dto.getCode()); // 예: msj***
		if (maskedId == null) {
			return ResponseEntity.badRequest().body(Map.of("message", "인증번호가 맞지 않거나 시간이 지났어요."));
		}
		return ResponseEntity.ok(Map.of("loginId", maskedId));
	}


	// 아이디·비밀번호 찾기 - 인증번호 확인 (M09)  POST /api/members/find/checkCode
	@PostMapping("/find/checkCode")
	public ResponseEntity<Map<String, Object>> checkCode(@RequestBody MemberFindDTO dto) {
		String error = service.checkCode(dto);
		if (error != null) {
			return ResponseEntity.badRequest().body(Map.of("message", error));
		}
		return ResponseEntity.ok(Map.of("message", "인증되었어요."));
	}

	// 비밀번호 찾기 - 새 비밀번호 저장 (M09)  POST /api/members/find/resetPw
	@PostMapping("/find/resetPw")
	public ResponseEntity<Map<String, Object>> resetPw(@RequestBody MemberFindDTO dto) {
		String error = service.resetPassword(dto);
		if (error != null) {
			return ResponseEntity.badRequest().body(Map.of("message", error));
		}
		return ResponseEntity.ok(Map.of("message", "비밀번호를 바꿨어요."));
	}
	
	

	// 내 특성 레벨 (C01 퀘스트 화면, M08 특성 강화)  GET /api/members/me/traits
	@GetMapping("/me/traits")
	public List<MemberTraitDTO> myTraits(@RequestAttribute("memberId") String memberId) {
		return service.myTraits(Integer.parseInt(memberId));
	}

	// 전생 기록 (M07)  GET /api/members/me/lives?page=1&sort=recent
	// sort: recent(최신순, 기본) / age(오래 산 순)
	@GetMapping("/me/lives")
	public Map<String, Object> myLives(@RequestAttribute("memberId") String memberId,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "recent") String sort) {
		return service.myLives(Integer.parseInt(memberId), page, sort);
	}

	// 특성 레벨 올리기 (M08)  POST /api/members/me/traits/{traitId}/levelUp
	// 포인트 1점 → 그 특성 1레벨 (다음 생부터 적용)
	@PostMapping("/me/traits/{traitId}/levelUp")
	public ResponseEntity<Map<String, Object>> levelUp(@RequestAttribute("memberId") String memberId,
			@PathVariable String traitId) {
		int id = Integer.parseInt(memberId);
		try {
			service.levelUp(id, traitId);
		} catch (IllegalStateException e) {
			// 포인트 부족 / 이미 Lv.5 → 아무것도 안 바뀜 (트랜잭션 취소)
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		}
		MyPageDTO my = service.myPage(id);
		if (my == null) {
			return ResponseEntity.status(404).body(Map.of("message", "회원 정보를 찾을 수 없어요."));
		}
		return ResponseEntity.ok(Map.of(
				"point", my.getPoint(),
				"traits", service.myTraits(id)));
	}
}
