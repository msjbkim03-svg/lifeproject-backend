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

	// C03 이어하기: 지금까지의 인생 기록  GET /api/engine/logs?lifeId=...&guestToken=...
	// (새로고침해도 진행 화면의 인생 기록이 비지 않게)
	@GetMapping("/api/engine/logs")
	public ResponseEntity<?> logs(@RequestParam int lifeId, @RequestParam(required = false) String guestToken,
			@RequestHeader(value = "Authorization", required = false) String auth) {
		java.util.List<EventLogDTO> list = engineService.myEventLogs(lifeId, loginMemberId(auth), guestToken);
		if (list == null) {
			return ResponseEntity.status(403).body("내 인생이 아니에요.");
		}
		return ResponseEntity.ok(list);
	}

	// C03 인생 지도: 한 해씩 남긴 기록  GET /api/engine/years?lifeId=...&guestToken=...
	@GetMapping("/api/engine/years")
	public ResponseEntity<?> years(@RequestParam int lifeId, @RequestParam(required = false) String guestToken,
			@RequestHeader(value = "Authorization", required = false) String auth) {
		java.util.List<YearLogDTO> list = engineService.myYearLogs(lifeId, loginMemberId(auth), guestToken);
		if (list == null) {
			return ResponseEntity.status(403).body("내 인생이 아니에요.");
		}
		return ResponseEntity.ok(list);
	}

	// C13 특성 칸 (진행 화면)  GET /api/engine/traits?lifeId=...&guestToken=...
	//   { traits: [{ traitId, level, effect }], temper: '호기심 많은' | null (2세에 고른 기질) }
	@GetMapping("/api/engine/traits")
	public ResponseEntity<?> traits(@RequestParam int lifeId, @RequestParam(required = false) String guestToken,
			@RequestHeader(value = "Authorization", required = false) String auth) {
		java.util.Map<String, Object> out = engineService.myTraits(lifeId, loginMemberId(auth), guestToken);
		if (out == null) {
			return ResponseEntity.status(403).body("내 인생이 아니에요.");
		}
		return ResponseEntity.ok(out);
	}

	// C03 나라 패널  GET /api/engine/country?lifeId=...&guestToken=...&view=cur|born
	@GetMapping("/api/engine/country")
	public ResponseEntity<?> country(@RequestParam int lifeId, @RequestParam(required = false) String guestToken,
			@RequestParam(required = false) String view,
			@RequestHeader(value = "Authorization", required = false) String auth) {
		CountryInfoDTO info = engineService.countryInfo(lifeId, loginMemberId(auth), guestToken, view);
		if (info == null) {
			return ResponseEntity.status(403).body("내 인생이 아니에요.");
		}
		return ResponseEntity.ok(info);
	}

	// C22 엔딩 크레딧 재료  GET /api/engine/film?lifeId=...&guestToken=...
	//   { life: 세상을 떠난 해의 모습(가족 포함), industry: { 나이: 산업 }, sources: [자료 이름] }
	@GetMapping("/api/engine/film")
	public ResponseEntity<?> film(@RequestParam int lifeId, @RequestParam(required = false) String guestToken,
			@RequestHeader(value = "Authorization", required = false) String auth) {
		java.util.Map<String, Object> out = engineService.filmData(lifeId, loginMemberId(auth), guestToken);
		if (out == null) {
			return ResponseEntity.status(403).body("내 인생이 아니에요.");
		}
		if (out.isEmpty()) {
			return ResponseEntity.badRequest().body("아직 끝나지 않은 인생이에요.");
		}
		return ResponseEntity.ok(out);
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

	// C06-3 돈 쓰기·투자 창  GET /api/engine/shop?lifeId=...&guestToken=...
	@GetMapping("/api/engine/shop")
	public ResponseEntity<?> shop(@RequestParam int lifeId, @RequestParam(required = false) String guestToken,
			@RequestHeader(value = "Authorization", required = false) String auth) {
		LifeDTO life = engineService.loadMyLife(lifeId, loginMemberId(auth), guestToken);
		if (life == null) {
			return ResponseEntity.status(403).body("내 인생이 아니에요.");
		}
		return ResponseEntity.ok(engineService.shop(life));
	}

	// C06-3 사기·하기  POST /api/engine/shop  { lifeId, itemId, atAge, guestToken }
	@PostMapping("/api/engine/shop")
	public ResponseEntity<?> buy(@RequestBody EngineRequestDTO req,
			@RequestHeader(value = "Authorization", required = false) String auth) {
		LifeDTO life = engineService.loadMyLife(req.getLifeId(), loginMemberId(auth), req.getGuestToken());
		if (life == null) {
			return ResponseEntity.status(403).body("내 인생이 아니에요.");
		}
		ShopDTO out = engineService.buy(life, req.getItemId(), req.getAtAge());
		if (out == null) {
			return ResponseEntity.badRequest().body(engineService.shopBlocked(life, req.getItemId()));
		}
		return ResponseEntity.ok(out);
	}

	// C03 잠시 멈춤 (원작 reflect): 5·15·30·50·70세에 적은 한 줄을 인생 기록에 남김
	//   POST /api/engine/note  { lifeId, text, atAge, guestToken }  → 남긴 문장 (따옴표 포함)
	@PostMapping("/api/engine/note")
	public ResponseEntity<?> note(@RequestBody EngineRequestDTO req,
			@RequestHeader(value = "Authorization", required = false) String auth) {
		LifeDTO life = engineService.loadMyLife(req.getLifeId(), loginMemberId(auth), req.getGuestToken());
		if (life == null) {
			return ResponseEntity.status(403).body("내 인생이 아니에요.");
		}
		String out = engineService.saveNote(life, req.getText(), req.getAtAge());
		if (out == null) {
			return ResponseEntity.badRequest().body("남길 글이 없거나 지금은 남길 수 없어요.");
		}
		return ResponseEntity.ok(out);
	}

	// C06-3 여유 자금 굴리기 방식  POST /api/engine/invest  { lifeId, investType, atAge, guestToken }
	@PostMapping("/api/engine/invest")
	public ResponseEntity<?> invest(@RequestBody EngineRequestDTO req,
			@RequestHeader(value = "Authorization", required = false) String auth) {
		LifeDTO life = engineService.loadMyLife(req.getLifeId(), loginMemberId(auth), req.getGuestToken());
		if (life == null) {
			return ResponseEntity.status(403).body("내 인생이 아니에요.");
		}
		ShopDTO out = engineService.invest(life, req.getInvestType(), req.getAtAge());
		if (out == null) {
			return ResponseEntity.badRequest().body("지금 고를 수 없는 투자 방식이에요.");
		}
		return ResponseEntity.ok(out);
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
		// 큰돈 드는 선택지인데 돈이 모자라면 (대출로도 안 되면) 고를 수 없음
		String blocked = engineService.choiceBlocked(life, req.getChoiceId());
		if (blocked != null) {
			return ResponseEntity.badRequest().body(blocked);
		}
		life = engineService.chooseEvent(life, req.getChoiceId(), req.getText()); // text: 반려동물 이름 등 (없으면 null)
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
