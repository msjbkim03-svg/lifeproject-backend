package com.example.demo.story;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.commons.JwtUtil;
import com.example.demo.engine.EngineService;
import com.example.demo.result.ResultDTO;
import com.example.demo.result.ResultService;

@RestController
public class StoryController {

	private final StoryService storyService;
	private final ResultService resultService;	// C18 결과(요약 + 인생 기록)를 이야기 재료로 씀
	private final EngineService engineService;	// 내 인생인지 확인 (isMyLife)
	private final JwtUtil jwtUtil;

	public StoryController(StoryService storyService, ResultService resultService,
			EngineService engineService, JwtUtil jwtUtil) {
		this.storyService = storyService;
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

	// C19 AI 인생 이야기  GET /api/story/{lifeId}?guestToken=...&scenes=16:grad,18:work,23:loss
	// 처음 부르면 Gemini로 만들어 저장(5~15초), 다음부터는 저장된 이야기를 바로 돌려줌
	// scenes: 화면이 장마다 붙이는 큰 장면 사진의 나이:장면 (그 장 글에 그 일이 꼭 나오게, 11/25)
	@GetMapping("/api/story/{lifeId}")
	public ResponseEntity<?> story(@PathVariable int lifeId,
			@RequestParam(required = false) String guestToken,
			@RequestParam(required = false) String scenes,
			@RequestHeader(value = "Authorization", required = false) String auth) {

		if (!engineService.isMyLife(lifeId, loginMemberId(auth), guestToken)) {
			return ResponseEntity.status(403).body("내 인생이 아니에요.");
		}
		ResultDTO result = resultService.getResult(lifeId);
		if (result == null) {
			return ResponseEntity.status(404).body("인생을 찾을 수 없어요.");
		}
		if (!"ENDED".equals(result.getStatus())) {
			return ResponseEntity.badRequest().body("인생이 끝나야 이야기를 만들 수 있어요.");
		}

		try {
			return ResponseEntity.ok(storyService.getOrCreateStory(result, scenes));
		} catch (AiCaller.AiFail f) {
			// AI가 안 되면 빈 화면 대신 인생 기록으로 엮은 이야기 + 까닭 안내 (원인은 AiCaller 가 콘솔에 한 줄)
			return ResponseEntity.ok(storyService.recordStory(result, f.userMessage, scenes));
		} catch (Exception e) {
			e.printStackTrace(); // DB 저장 등 AI 밖의 문제
			return ResponseEntity.status(500).body("이야기를 불러오지 못했어요. 잠시 후 다시 눌러 주세요.");
		}
	}
}
