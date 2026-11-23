package com.example.demo.costume;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.commons.JwtUtil;

// 비회원 인생도 쓰니까 LoginValidator 에 넣지 않음 → 토큰은 있으면 직접 확인
@RestController
@RequestMapping("/api/costumes")
public class CostumeController {

	private CostumeService service;
	private JwtUtil jwt;

	public CostumeController(CostumeService service, JwtUtil jwt) {
		this.service = service;
		this.jwt = jwt;
	}

	@GetMapping
	public ResponseEntity<?> list(@RequestParam int lifeId, @RequestParam(required = false) String guestToken,
			@RequestHeader(value = "Authorization", required = false) String auth) {
		CostumeLifeDTO life = service.myLife(lifeId, readMemberId(auth), guestToken);
		if (life == null) return ResponseEntity.status(403).body(Map.of("message", "내 인생이 아니에요."));
		return ResponseEntity.ok(service.list(life));
	}

	@PostMapping("/{costumeId}/buy")
	public ResponseEntity<?> buy(@PathVariable int costumeId, @RequestBody Map<String, Object> body,
			@RequestHeader(value = "Authorization", required = false) String auth) {
		return change(costumeId, body, auth, "buy");
	}

	@PostMapping("/{costumeId}/equip")
	public ResponseEntity<?> equip(@PathVariable int costumeId, @RequestBody Map<String, Object> body,
			@RequestHeader(value = "Authorization", required = false) String auth) {
		return change(costumeId, body, auth, "equip");
	}

	@PostMapping("/{costumeId}/unequip")
	public ResponseEntity<?> unequip(@PathVariable int costumeId, @RequestBody Map<String, Object> body,
			@RequestHeader(value = "Authorization", required = false) String auth) {
		return change(costumeId, body, auth, "unequip");
	}

	// 사기·착용·벗기 공통: 주인 확인 → 처리 → 바뀐 목록 + 안내 문장
	private ResponseEntity<?> change(int costumeId, Map<String, Object> body, String auth, String action) {
		int lifeId = Integer.parseInt(String.valueOf(body.get("lifeId")));
		String guestToken = (String) body.get("guestToken");
		Integer memberId = readMemberId(auth);
		CostumeLifeDTO life = service.myLife(lifeId, memberId, guestToken);
		if (life == null) return ResponseEntity.status(403).body(Map.of("message", "내 인생이 아니에요."));

		String error;
		String done;
		if (action.equals("buy")) {
			error = service.buy(life, costumeId);
			done = "사서 바로 착용했어요.";
		} else if (action.equals("equip")) {
			error = service.equip(life, costumeId);
			done = "착용했어요.";
		} else {
			error = service.unequip(life, costumeId);
			done = "벗었어요.";
		}
		if (error != null) return ResponseEntity.badRequest().body(Map.of("message", error));

		Map<String, Object> result = service.list(service.myLife(lifeId, memberId, guestToken));
		result.put("message", done);
		return ResponseEntity.ok(result);
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
