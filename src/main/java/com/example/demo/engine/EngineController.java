package com.example.demo.engine;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.commons.JwtUtil;

@RestController
public class EngineController {

	private final EngineDao engineDao;
	private final EngineService engineService;
	private final JwtUtil jwtUtil;

	// 생성자 주입: 스프링이 객체를 만들 때 EngineDao, EngineService, JwtUtil을 넣어줌
	public EngineController(EngineDao engineDao, EngineService engineService, JwtUtil jwtUtil) {
		this.engineDao = engineDao;
		this.engineService = engineService;
		this.jwtUtil = jwtUtil;
	}

	// 로그인 토큰에서 회원 번호 꺼내기 (없거나 잘못된 토큰이면 null = 비회원)
	// React에서 보내는 헤더: Authorization: Bearer 토큰
	private Integer loginMemberId(String auth) {
		if (auth == null || !auth.startsWith("Bearer ")) {
			return null;
		}
		try {
			String id = jwtUtil.verifyToken(auth.substring(7)).getSubject();
			return Integer.parseInt(id);
		} catch (Exception e) {
			return null; // 만료·위조된 토큰
		}
	}

	// DB 연결 테스트
	@GetMapping("/api/test/db")
	public String dbTest() {
		return "DB 연결 성공: " + engineDao.selectNow();
	}

	// C02 랜덤 출생  POST /api/engine/birth  { decade, guestToken }
	// 회원: Authorization 헤더 / 비회원: 예전 guestToken (있으면 그 인생은 포기 처리)
	@PostMapping("/api/engine/birth")
	public ResponseEntity<?> birth(@RequestBody EngineRequestDTO req,
			@RequestHeader(value = "Authorization", required = false) String auth) {
		int decade = req.getDecade();
		String guestToken = req.getGuestToken();

		// 연대는 1940 ~ 현재 연대, 10년 단위만 허용
		int lastDecade = LocalDate.now().getYear() / 10 * 10;
		if (decade < 1940 || decade > lastDecade || decade % 10 != 0) {
			return ResponseEntity.badRequest()
					.body("연대는 1940 ~ " + lastDecade + " 사이 10년 단위로 보내주세요.");
		}

		LifeDTO life = engineService.birth(decade, guestToken, loginMemberId(auth));
		if (life == null) {
			return ResponseEntity.status(404)
					.body(decade + "년대 통계 데이터가 없어서 태어날 수 없어요.");
		}
		return ResponseEntity.ok(life);
	}

	// C03 이어하기: 진행 중인 인생 불러오기  GET /api/engine/current?guestToken=...
	@GetMapping("/api/engine/current")
	public ResponseEntity<?> current(@RequestParam(required = false) String guestToken,
			@RequestHeader(value = "Authorization", required = false) String auth) {
		LifeDTO life = engineService.currentLife(loginMemberId(auth), guestToken);
		if (life == null) {
			return ResponseEntity.status(404).body("진행 중인 인생이 없어요.");
		}
		return ResponseEntity.ok(life);
	}

	// C03 1년 진행  POST /api/engine/next  { lifeId, guestToken }
	@PostMapping("/api/engine/next")
	public ResponseEntity<?> nextYear(@RequestBody EngineRequestDTO req,
			@RequestHeader(value = "Authorization", required = false) String auth) {
		// 인생은 여기서 1번만 읽고 서비스에 넘김 (DB 왕복 줄이기)
		LifeDTO life = engineService.loadMyLife(req.getLifeId(), loginMemberId(auth), req.getGuestToken());
		if (life == null) {
			return ResponseEntity.status(403).body("내 인생이 아니에요.");
		}
		if (engineService.isWaitingChoice(life)) {
			return ResponseEntity.status(409).body("선택 이벤트를 먼저 골라 주세요.");
		}
		life = engineService.nextYear(life);
		if (life == null) {
			return ResponseEntity.status(409)
					.body("이미 끝났거나 방금 진행된 해예요. 새로고침해 주세요.");
		}
		return ResponseEntity.ok(life);
	}

	// C04 집중 선택  POST /api/engine/focus  { lifeId, focus, guestToken }
	@PostMapping("/api/engine/focus")
	public ResponseEntity<?> chooseFocus(@RequestBody EngineRequestDTO req,
			@RequestHeader(value = "Authorization", required = false) String auth) {
		LifeDTO life = engineService.loadMyLife(req.getLifeId(), loginMemberId(auth), req.getGuestToken());
		if (life == null) {
			return ResponseEntity.status(403).body("내 인생이 아니에요.");
		}
		life = engineService.chooseFocus(life, req.getFocus());
		if (life == null) {
			return ResponseEntity.badRequest()
					.body("지금 나이대에서 고를 수 없는 집중이거나, 진행 중인 인생이 아니에요.");
		}
		return ResponseEntity.ok(life);
	}

	// C06 직업 선택  POST /api/engine/job  { lifeId, jobId, guestToken }
	@PostMapping("/api/engine/job")
	public ResponseEntity<?> chooseJob(@RequestBody EngineRequestDTO req,
			@RequestHeader(value = "Authorization", required = false) String auth) {
		LifeDTO life = engineService.loadMyLife(req.getLifeId(), loginMemberId(auth), req.getGuestToken());
		if (life == null) {
			return ResponseEntity.status(403).body("내 인생이 아니에요.");
		}
		life = engineService.chooseJob(life, req.getJobId());
		if (life == null) {
			return ResponseEntity.badRequest()
					.body("지금은 직업을 고를 수 없거나, 올해 제안받은 직업이 아니에요.");
		}
		return ResponseEntity.ok(life);
	}

	// C09 선택 이벤트 고르기  POST /api/engine/choice  { lifeId, choiceId, guestToken }
	@PostMapping("/api/engine/choice")
	public ResponseEntity<?> chooseEvent(@RequestBody EngineRequestDTO req,
			@RequestHeader(value = "Authorization", required = false) String auth) {
		LifeDTO life = engineService.loadMyLife(req.getLifeId(), loginMemberId(auth), req.getGuestToken());
		if (life == null) {
			return ResponseEntity.status(403).body("내 인생이 아니에요.");
		}
		life = engineService.chooseEvent(life, req.getChoiceId());
		if (life == null) {
			return ResponseEntity.badRequest()
					.body("지금 고를 선택 이벤트가 없거나, 이 이벤트의 선택지가 아니에요.");
		}
		return ResponseEntity.ok(life);
	}

	// C10 이민 목적지 고르기  POST /api/engine/destination  { lifeId, countryCode, guestToken }
	// '떠난다'를 고른 응답에 needDestination = true, destinations = 후보 나라 목록이 오면 그중 하나를 보냄
	@PostMapping("/api/engine/destination")
	public ResponseEntity<?> chooseDestination(@RequestBody EngineRequestDTO req,
			@RequestHeader(value = "Authorization", required = false) String auth) {
		LifeDTO life = engineService.loadMyLife(req.getLifeId(), loginMemberId(auth), req.getGuestToken());
		if (life == null) {
			return ResponseEntity.status(403).body("내 인생이 아니에요.");
		}
		life = engineService.chooseDestination(life, req.getCountryCode());
		if (life == null) {
			return ResponseEntity.badRequest()
					.body("지금 목적지를 고를 때가 아니거나, 후보에 없는 나라예요.");
		}
		return ResponseEntity.ok(life);
	}
}
