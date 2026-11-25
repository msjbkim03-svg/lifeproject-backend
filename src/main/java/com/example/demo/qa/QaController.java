package com.example.demo.qa;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.commons.JwtUtil;
import com.example.demo.engine.EngineService;
import com.example.demo.result.ResultDTO;
import com.example.demo.result.ResultService;
import com.example.demo.story.AiCaller;

@RestController
public class QaController {

	private final QaService qaService;
	private final ResultService resultService;	// C18 결과(요약 + 인생 기록)를 근거로 씀
	private final EngineService engineService;	// 내 인생인지 확인 (isMyLife)
	private final JwtUtil jwtUtil;

	public QaController(QaService qaService, ResultService resultService,
			EngineService engineService, JwtUtil jwtUtil) {
		this.qaService = qaService;
		this.resultService = resultService;
		this.engineService = engineService;
		this.jwtUtil = jwtUtil;
	}

	// 로그인 토큰에서 회원 번호 꺼내기 (없거나 잘못된 토큰이면 null = 비회원)
	private Integer loginMemberId(String auth) {
		if (auth == null || !auth.startsWith("Bearer ")) {
			return null;
		}
		try {
			String id = jwtUtil.verifyToken(auth.substring(7)).getSubject();
			return Integer.parseInt(id);
		} catch (Exception e) {
			return null;
		}
	}

	// 지난 질문·답변 목록  GET /api/qa/{lifeId}?guestToken=...
	@GetMapping("/api/qa/{lifeId}")
	public ResponseEntity<?> list(@PathVariable int lifeId,
			@RequestParam(required = false) String guestToken,
			@RequestHeader(value = "Authorization", required = false) String auth) {
		if (!engineService.isMyLife(lifeId, loginMemberId(auth), guestToken)) {
			return ResponseEntity.status(403).body("내 인생이 아니에요.");
		}
		return ResponseEntity.ok(qaService.list(lifeId));
	}

	// C20 질문하기  POST /api/qa/{lifeId}   { "question": "...", "guestToken": "..." }
	@PostMapping("/api/qa/{lifeId}")
	public ResponseEntity<?> ask(@PathVariable int lifeId, @RequestBody QaRequestDTO req,
			@RequestHeader(value = "Authorization", required = false) String auth) {

		// 1. 내 인생, 끝난 인생인지
		if (!engineService.isMyLife(lifeId, loginMemberId(auth), req.getGuestToken())) {
			return ResponseEntity.status(403).body("내 인생이 아니에요.");
		}
		ResultDTO result = resultService.getResult(lifeId);
		if (result == null) {
			return ResponseEntity.status(404).body("인생을 찾을 수 없어요.");
		}
		if (!"ENDED".equals(result.getStatus())) {
			return ResponseEntity.badRequest().body("인생이 끝나야 질문할 수 있어요.");
		}

		// 2. 질문 확인 (빈 질문, 너무 긴 질문, 질문 수 제한)
		String question = req.getQuestion() == null ? "" : req.getQuestion().trim();
		if (question.isEmpty()) {
			return ResponseEntity.badRequest().body("질문을 입력해 주세요.");
		}
		if (question.length() > QaService.MAX_QUESTION_LENGTH) {
			return ResponseEntity.badRequest().body("질문은 " + QaService.MAX_QUESTION_LENGTH + "자까지 쓸 수 있어요.");
		}
		if (qaService.count(lifeId) >= QaService.MAX_QUESTIONS) {
			return ResponseEntity.badRequest().body("이 인생에는 질문을 " + QaService.MAX_QUESTIONS + "개까지 할 수 있어요.");
		}

		// 3. 답변 만들기
		try {
			return ResponseEntity.ok(qaService.ask(result, question, req.getSinceQaId()));
		} catch (AiCaller.AiFail f) {
			// 원인별 안내 (한도 초과 · 키 문제 · 시간 초과 …), 원인은 AiCaller 가 콘솔에 한 줄
			return ResponseEntity.status(503).body(f.userMessage);
		} catch (Exception e) {
			e.printStackTrace(); // DB 저장 등 AI 밖의 문제
			return ResponseEntity.status(500).body("답을 저장하지 못했어요. 잠시 후 다시 물어봐 주세요.");
		}
	}
}
