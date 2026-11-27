package com.example.demo.quest;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// ※ 회원만 씀 → WebConfig 에서 /api/quests/** 에 LoginValidator(로그인 검사) 적용
@RestController
@RequestMapping("/api/quests")
public class QuestController {

	private QuestService service;

	public QuestController(QuestService service) {
		this.service = service;
	}

	// 퀘스트 목록  GET /api/quests
	@GetMapping
	public List<QuestDTO> list() {
		return service.list();
	}

	// 고른 퀘스트 저장  POST /api/quests/life
	@PostMapping("/life")
	public ResponseEntity<Map<String, Object>> saveLifeQuests(@RequestAttribute("memberId") String memberId,
			@RequestBody QuestSaveDTO dto) {
		try {
			String error = service.saveLifeQuests(Integer.parseInt(memberId), dto);
			if (error != null) {
				return ResponseEntity.badRequest().body(Map.of("message", error));
			}
			return ResponseEntity.ok(Map.of("message", "퀘스트를 정했어요."));
		} catch (DuplicateKeyException e) {
			// 거의 동시에 두 번 요청한 경우 → DB 기본키(LIFE_ID, QUEST_ID) 제약이 막아 줌
			return ResponseEntity.badRequest().body(Map.of("message", "이 인생의 퀘스트는 이미 정해졌어요."));
		}
	}
}
