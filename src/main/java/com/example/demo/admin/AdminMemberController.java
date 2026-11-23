package com.example.demo.admin;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// A01 관리자 회원 관리
// ※ 로그인 검사(LoginValidator)와 관리자 검사(AdminValidator)는 WebConfig 가 /api/admin/** 에 이미 걸어 둠
//   → 여기까지 왔다면 관리자라는 뜻이라 Controller 에서 따로 확인하지 않음
@RestController
@RequestMapping("/api/admin/members")
public class AdminMemberController {

	private final AdminMemberService service;

	public AdminMemberController(AdminMemberService service) {
		this.service = service;
	}

	// 회원 목록  GET /api/admin/members?keyword=민승&status=ACTIVE&page=1
	@GetMapping
	public Map<String, Object> list(@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String status,
			@RequestParam(defaultValue = "1") int page) {
		return service.list(keyword, status, page);
	}

	// 강제 탈퇴  POST /api/admin/members/{memberId}/withdraw
	@PostMapping("/{memberId}/withdraw")
	public ResponseEntity<Map<String, Object>> withdraw(@PathVariable int memberId) {
		String error = service.withdraw(memberId);
		if (error != null) {
			return ResponseEntity.badRequest().body(Map.of("message", error));
		}
		return ResponseEntity.ok(Map.of("message", "탈퇴 처리했어요."));
	}
}
