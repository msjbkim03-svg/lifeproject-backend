package com.example.demo.admin;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// A02 관리자 채팅·방명록·금칙어 관리
// ※ 로그인 검사(LoginValidator)와 관리자 검사(AdminValidator)는 WebConfig 가 /api/admin/** 에 이미 걸어 둠
@RestController
@RequestMapping("/api/admin/chat")
public class AdminChatController {

	private final AdminChatService service;

	public AdminChatController(AdminChatService service) {
		this.service = service;
	}

	// 1-1. 금칙어 목록  GET /api/admin/chat/words
	@GetMapping("/words")
	public List<String> words() {
		return service.words();
	}

	// 1-2. 금칙어 추가  POST /api/admin/chat/words  { "word": "바보" }
	@PostMapping("/words")
	public ResponseEntity<Map<String, Object>> addWord(@RequestBody Map<String, String> body) {
		String error = service.addWord(body == null ? null : body.get("word"));
		if (error != null) {
			return ResponseEntity.badRequest().body(Map.of("message", error));
		}
		return ResponseEntity.ok(Map.of("message", "금칙어를 추가했어요.", "words", service.words()));
	}

	// 1-3. 금칙어 삭제  DELETE /api/admin/chat/words?word=바보
	@DeleteMapping("/words")
	public ResponseEntity<Map<String, Object>> removeWord(@RequestParam String word) {
		String error = service.removeWord(word);
		if (error != null) {
			return ResponseEntity.badRequest().body(Map.of("message", error));
		}
		return ResponseEntity.ok(Map.of("message", "금칙어를 삭제했어요.", "words", service.words()));
	}

	// 2-1. 마을 채팅 목록  GET /api/admin/chat/messages?keyword=...&status=N&page=1
	@GetMapping("/messages")
	public Map<String, Object> chatList(@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String status,
			@RequestParam(defaultValue = "1") int page) {
		return service.chatList(keyword, status, page);
	}

	// 2-2. 마을 채팅 삭제  DELETE /api/admin/chat/messages/{msgId}
	@DeleteMapping("/messages/{msgId}")
	public ResponseEntity<Map<String, Object>> removeChat(@PathVariable int msgId) {
		String error = service.removeChat(msgId);
		if (error != null) {
			return ResponseEntity.badRequest().body(Map.of("message", error));
		}
		return ResponseEntity.ok(Map.of("message", "채팅 메시지를 삭제 처리했어요."));
	}

	// 3-1. 추모관 방명록 목록  GET /api/admin/chat/comments?keyword=...&page=1
	@GetMapping("/comments")
	public Map<String, Object> commentList(@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") int page) {
		return service.commentList(keyword, page);
	}

	// 3-2. 추모관 방명록 삭제  DELETE /api/admin/chat/comments/{commentId}
	@DeleteMapping("/comments/{commentId}")
	public ResponseEntity<Map<String, Object>> removeComment(@PathVariable int commentId) {
		String error = service.removeComment(commentId);
		if (error != null) {
			return ResponseEntity.badRequest().body(Map.of("message", error));
		}
		return ResponseEntity.ok(Map.of("message", "방명록 댓글을 삭제했어요."));
	}

	// 4-1. 추모관 글 목록  GET /api/admin/chat/posts?keyword=...&page=1
	@GetMapping("/posts")
	public Map<String, Object> postList(@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") int page) {
		return service.postList(keyword, page);
	}

	// 4-2. 추모관 글 삭제 (달린 방명록·촛불 포함)  DELETE /api/admin/chat/posts/{postId}
	@DeleteMapping("/posts/{postId}")
	public ResponseEntity<Map<String, Object>> removePost(@PathVariable int postId) {
		String error = service.removePost(postId);
		if (error != null) {
			return ResponseEntity.badRequest().body(Map.of("message", error));
		}
		return ResponseEntity.ok(Map.of("message", "추모관 글과 달린 방명록을 삭제했어요."));
	}
}
