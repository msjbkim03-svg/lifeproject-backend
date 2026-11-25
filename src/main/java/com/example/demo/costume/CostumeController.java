package com.example.demo.costume;

import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
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
	public ResponseEntity<Map<String, Object>> list(@RequestParam int lifeId, @RequestParam(required = false) String guestToken,
			@RequestHeader(value = "Authorization", required = false) String auth) {
		CostumeLifeDTO life = service.myLife(lifeId, jwt.memberIdOfHeader(auth), guestToken);
		if (life == null) return ResponseEntity.status(403).body(Map.of("message", "내 인생이 아니에요."));
		return ResponseEntity.ok(service.list(life));
	}

	@PostMapping("/{costumeId}/buy")
	public ResponseEntity<Map<String, Object>> buy(@PathVariable int costumeId, @RequestBody Map<String, Object> body,
			@RequestHeader(value = "Authorization", required = false) String auth) {
		return change(costumeId, body, auth, "buy");
	}

	@PostMapping("/{costumeId}/equip")
	public ResponseEntity<Map<String, Object>> equip(@PathVariable int costumeId, @RequestBody Map<String, Object> body,
			@RequestHeader(value = "Authorization", required = false) String auth) {
		return change(costumeId, body, auth, "equip");
	}

	@PostMapping("/{costumeId}/unequip")
	public ResponseEntity<Map<String, Object>> unequip(@PathVariable int costumeId, @RequestBody Map<String, Object> body,
			@RequestHeader(value = "Authorization", required = false) String auth) {
		return change(costumeId, body, auth, "unequip");
	}

	// 사기·착용·벗기 공통: 주인 확인 → 처리 → 바뀐 목록 + 안내 문장
	private ResponseEntity<Map<String, Object>> change(int costumeId, Map<String, Object> body, String auth, String action) {
		Integer lifeId = toInt(body.get("lifeId"));
		if (lifeId == null) return ResponseEntity.badRequest().body(Map.of("message", "인생 번호가 없어요."));
		// 글자가 아닌 값이 와도 500 에러가 나지 않게 (없으면 null)
		Object guest = body.get("guestToken");
		String guestToken = guest == null ? null : String.valueOf(guest);
		Integer memberId = jwt.memberIdOfHeader(auth);
		CostumeLifeDTO life = service.myLife(lifeId, memberId, guestToken);
		if (life == null) return ResponseEntity.status(403).body(Map.of("message", "내 인생이 아니에요."));

		String error;
		String done;
		if (action.equals("buy")) {
			try {
				error = service.buy(life, costumeId);
			} catch (DuplicateKeyException e) {
				// 거의 동시에 두 번 삼 → DB 제약이 막아 줌 (두 번째 돈 차감도 같이 취소됨)
				error = "이미 가진 물건이에요.";
			}
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

	// 숫자로 바꾸기 (없거나 숫자가 아니면 null)
	private Integer toInt(Object value) {
		if (value == null) return null;
		try {
			return Integer.parseInt(String.valueOf(value));
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
