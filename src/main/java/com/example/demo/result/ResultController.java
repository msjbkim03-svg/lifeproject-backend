package com.example.demo.result;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.commons.JwtUtil;
import com.example.demo.engine.EngineService;

@RestController
public class ResultController {

	private final ResultService resultService;
	private final EngineService engineService;	// 내 인생인지 확인 (isMyLife) 은 엔진 것을 같이 씀
	private final JwtUtil jwtUtil;

	public ResultController(ResultService resultService, EngineService engineService, JwtUtil jwtUtil) {
		this.resultService = resultService;
		this.engineService = engineService;
		this.jwtUtil = jwtUtil;
	}

	// 로그인 토큰에서 회원 번호 꺼내기 (없거나 잘못된 토큰이면 null = 비회원) - EngineController와 같은 방식
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

	// C18 인생 결과  GET /api/result/{lifeId}?guestToken=...
	// 끝난 인생(ENDED)은 공유 링크·추모관에서도 누구나 볼 수 있게 공개 (주인 여부는 result.mine 으로 전달, 11/27 동현님)
	@GetMapping("/api/result/{lifeId}")
	public ResponseEntity<?> result(@PathVariable int lifeId,
			@RequestParam(required = false) String guestToken,
			@RequestHeader(value = "Authorization", required = false) String auth) {

		// 1. 결과 모으기
		ResultDTO result = resultService.getResult(lifeId);
		if (result == null) {
			return ResponseEntity.status(404).body("인생을 찾을 수 없어요.");
		}

		// 2. 아직 진행 중이면 결과 없음
		if (!"ENDED".equals(result.getStatus())) {
			return ResponseEntity.badRequest().body("아직 끝나지 않은 인생이에요.");
		}

		// 3. 내 인생 여부 표시 (추모관에 올리기 등은 주인만 표시)
		result.setMine(engineService.isMyLife(lifeId, loginMemberId(auth), guestToken));
		return ResponseEntity.ok(result);
	}
}
