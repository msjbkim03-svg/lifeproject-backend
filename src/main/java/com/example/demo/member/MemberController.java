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
	public Map<String, Boolean> checkNickname(@RequestParam String nickname) {
		return Map.of("available", service.isNicknameAvailable(nickname));
	}

	// 이메일 중복 확인  GET /api/members/checkEmail?email=a@b.com
	@GetMapping("/checkEmail")
	public Map<String, Boolean> checkEmail(@RequestParam String email) {
		return Map.of("available", service.isEmailAvailable(email));
	}

	// 로그인 (M02)  POST /api/members/login
	@PostMapping("/login")
	public ResponseEntity<Map<String, Object>> login(@RequestBody MemberLoginDTO dto) {
		MemberDTO member = service.login(dto.getLoginId(), dto.getPassword());

		if (member == null) {
			return ResponseEntity.status(401)
					.body(Map.of("message", "아이디 또는 비밀번호가 맞지 않아요."));
		}

		// 이 브라우저에서 비회원으로 하던 인생이 있으면 회원 인생으로 전환
		boolean converted = service.convertGuestLife(member.getMemberId(), dto.getGuestToken());
		
		String token = jwtUtil.createToken(String.valueOf(member.getMemberId()),member.getMemberRole());

		return ResponseEntity.ok(Map.of(
				"token", token,
				"nickname", member.getNickname(),
				"memberRole", member.getMemberRole(),
				"guestConverted", converted));
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
		boolean ok = service.withdraw(Integer.parseInt(memberId), dto.getCurrentPassword());
		if (!ok) {
			return ResponseEntity.badRequest().body(Map.of("message", "비밀번호가 맞지 않아요."));
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
		return ResponseEntity.ok(Map.of(
				"point", service.myPage(id).getPoint(),
				"traits", service.myTraits(id)));
	}
}
