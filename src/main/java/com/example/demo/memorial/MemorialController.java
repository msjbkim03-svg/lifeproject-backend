package com.example.demo.memorial;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.commons.JwtUtil;

// open·candle·comments 는 WebConfig 에서 LoginValidator 적용 (로그인 필요)
@RestController
@RequestMapping("/api/memorials")
public class MemorialController {

	private MemorialService service;
	private JwtUtil jwt;

	public MemorialController(MemorialService service, JwtUtil jwt) {
		this.service = service;
		this.jwt = jwt;
	}

	@GetMapping
	public Map<String, Object> list(@RequestParam(defaultValue = "1") int page) {
		return service.list(page);
	}

	// 로그인은 선택: 토큰이 있으면 내 촛불·내 방명록 표시
	@GetMapping("/{lifeId}")
	public ResponseEntity<MemorialRoomDTO> room(@PathVariable int lifeId,@RequestHeader(value = "Authorization", required = false) String auth) {
		MemorialRoomDTO room = service.room(lifeId, readMemberId(auth));
		if (room == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(room);
	}

	@PostMapping("/{lifeId}/open")
	public ResponseEntity<Map<String, Object>> open(@PathVariable int lifeId, @RequestAttribute("memberId") String memberId,
			@RequestBody Map<String, String> body) {
		String error = service.open(lifeId, Integer.parseInt(memberId), body.get("message"));
		if (error != null) return ResponseEntity.badRequest().body(Map.of("message", error));
		return ResponseEntity.ok(Map.of("message", "추모관을 열었어요."));
	}

	@PostMapping("/{lifeId}/candle")
	public ResponseEntity<Map<String, Object>> candle(@PathVariable int lifeId, @RequestAttribute("memberId") String memberId) {
		String error = service.candle(lifeId, Integer.parseInt(memberId));
		if (error != null) return ResponseEntity.badRequest().body(Map.of("message", error));
		return ResponseEntity.ok(Map.of("tributeCount", service.tributeCount(lifeId)));
	}

	@PostMapping("/{lifeId}/comments")
	public ResponseEntity<Map<String, Object>> comment(@PathVariable int lifeId, @RequestAttribute("memberId") String memberId,
			@RequestBody Map<String, String> body) {
		String error = service.comment(lifeId, Integer.parseInt(memberId), body.get("content"));
		if (error != null) return ResponseEntity.badRequest().body(Map.of("message", error));
		return ResponseEntity.ok(Map.of("message", "방명록을 남겼어요."));
	}

	@DeleteMapping("/comments/{commentId}")
	public ResponseEntity<Map<String, Object>> deleteComment(@PathVariable int commentId, @RequestAttribute("memberId") String memberId) {
		if (!service.deleteComment(commentId, Integer.parseInt(memberId))) {
			return ResponseEntity.badRequest().body(Map.of("message", "내가 쓴 방명록만 지울 수 있어요."));
		}
		return ResponseEntity.ok(Map.of("message", "방명록을 지웠어요."));
	}

	private Integer readMemberId(String auth) {
		if (auth == null || !auth.startsWith("Bearer ")) return null;
		try {
			return Integer.parseInt(jwt.verifyToken(auth.substring(7)).getSubject());
		} catch (Exception e) {
			return null;
		}
	}
}
