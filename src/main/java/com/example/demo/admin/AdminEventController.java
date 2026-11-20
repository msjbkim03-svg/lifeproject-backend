package com.example.demo.admin;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.event.dto.EventChoiceDTO;
import com.example.demo.event.dto.EventTemplateDTO;
import com.example.demo.event.dto.GameEventDTO;

// A03 이벤트 관리 (관리자 확인은 AdminValidator 가 /api/admin/** 에서 함)
@RestController
@RequestMapping("/api/admin")
public class AdminEventController {

	private final AdminEventService aServ;
	private final AdminDraftService dServ; // C10 역사 문장 초안·검수

	public AdminEventController(AdminEventService aServ, AdminDraftService dServ) {
		this.aServ = aServ;
		this.dServ = dServ;
	}

	// 목록 GET /api/admin/events?type=HIST&reviewed=N&aiNarrate=Y&keyword=전쟁
	@GetMapping("/events")
	public ResponseEntity<List<AdminEventRowDTO>> list(@RequestParam(required = false) String type,
			@RequestParam(required = false) String reviewed, @RequestParam(required = false) String aiNarrate,
			@RequestParam(required = false) String keyword) {
		return ResponseEntity.ok(aServ.list(type, reviewed, aiNarrate, keyword));
	}

	// 상세 GET /api/admin/events/{eventId}
	@GetMapping("/events/{eventId}")
	public ResponseEntity<?> detail(@PathVariable String eventId) {
		Map<String, Object> d = aServ.detail(eventId);
		if (d == null) {
			return ResponseEntity.status(404).body("없는 이벤트예요.");
		}
		return ResponseEntity.ok(d);
	}

	// 등록 POST /api/admin/events (body: GameEventDTO JSON)
	@PostMapping("/events")
	public ResponseEntity<?> create(@RequestBody GameEventDTO e) {
		try {
			aServ.createEvent(e);
			return ResponseEntity.ok(Map.of("eventId", e.getEventId()));
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(ex.getMessage());
		} catch (DuplicateKeyException ex) { // DataIntegrityViolationException 의 자식이라 먼저 잡아야 함
			return ResponseEntity.status(409).body("이미 있는 이벤트 ID예요.");
		} catch (DataIntegrityViolationException ex) {
			return ResponseEntity.badRequest().body("템플릿·역사 사건·사건 유형 ID를 확인해 주세요.");
		}
	}

	// 수정 PUT /api/admin/events/{eventId} (body: 상세 조회의 event 를 고친 것 전체)
	@PutMapping("/events/{eventId}")
	public ResponseEntity<?> update(@PathVariable String eventId, @RequestBody GameEventDTO e) {
		try {
			if (!aServ.updateEvent(eventId, e)) {
				return ResponseEntity.status(404).body("없는 이벤트예요.");
			}
			return ResponseEntity.ok(Map.of("eventId", eventId));
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(ex.getMessage());
		} catch (DataIntegrityViolationException ex) {
			return ResponseEntity.badRequest().body("템플릿·역사 사건·사건 유형 ID를 확인해 주세요.");
		}
	}

	// AI 서술 지정 PUT /api/admin/events/{eventId}/ai-narrate?value=Y
	@PutMapping("/events/{eventId}/ai-narrate")
	public ResponseEntity<?> aiNarrate(@PathVariable String eventId, @RequestParam String value) {
		try {
			if (!aServ.setAiNarrate(eventId, value)) {
				return ResponseEntity.status(404).body("없는 이벤트예요.");
			}
			return ResponseEntity.ok(Map.of("eventId", eventId, "aiNarrate", value));
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(ex.getMessage());
		}
	}

	// ================= 선택지 =================

	// 결과 동작 목록 GET /api/admin/result-actions
	@GetMapping("/result-actions")
	public ResponseEntity<List<String>> resultActions() {
		return ResponseEntity.ok(aServ.resultActions());
	}

	// 선택지 추가 POST /api/admin/events/{eventId}/choices
	@PostMapping("/events/{eventId}/choices")
	public ResponseEntity<?> createChoice(@PathVariable String eventId, @RequestBody EventChoiceDTO c) {
		try {
			EventChoiceDTO saved = aServ.createChoice(eventId, c);
			if (saved == null) {
				return ResponseEntity.status(404).body("없는 이벤트예요.");
			}
			return ResponseEntity.ok(Map.of("choiceId", saved.getChoiceId(), "choiceOrder", saved.getChoiceOrder()));
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(ex.getMessage());
		} catch (DuplicateKeyException ex) {
			return ResponseEntity.status(409).body("선택지 번호가 기존 번호와 겹쳐요. 시퀀스를 확인해 주세요.");
		} catch (DataIntegrityViolationException ex) {
			return ResponseEntity.badRequest().body("이민 나라 코드를 확인해 주세요.");
		}
	}

	// 선택지 수정 PUT /api/admin/choices/{choiceId} (순서·이벤트는 안 바뀜)
	@PutMapping("/choices/{choiceId}")
	public ResponseEntity<?> updateChoice(@PathVariable int choiceId, @RequestBody EventChoiceDTO c) {
		try {
			if (!aServ.updateChoice(choiceId, c)) {
				return ResponseEntity.status(404).body("없는 선택지예요.");
			}
			return ResponseEntity.ok(Map.of("choiceId", choiceId));
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(ex.getMessage());
		} catch (DataIntegrityViolationException ex) {
			return ResponseEntity.badRequest().body("이민 나라 코드를 확인해 주세요.");
		}
	}

	// ================= 템플릿 확률 조정 =================

	// 템플릿 목록 GET /api/admin/templates
	@GetMapping("/templates")
	public ResponseEntity<List<EventTemplateDTO>> templates() {
		return ResponseEntity.ok(aServ.templates());
	}

	// 확률·나이 수정 PUT /api/admin/templates/{templateId} (body: baseProb, minAge,
	// maxAge)
	@PutMapping("/templates/{templateId}")
	public ResponseEntity<?> updateTemplate(@PathVariable String templateId, @RequestBody EventTemplateDTO t) {
		try {
			EventTemplateDTO saved = aServ.updateTemplate(templateId, t);
			if (saved == null) {
				return ResponseEntity.status(404).body("없는 템플릿이에요.");
			}
			return ResponseEntity.ok(saved);
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(ex.getMessage());
		}
	}

	// ================= C10 역사 문장 초안·검수 =================

	// AI 초안 POST /api/admin/hist-drafts (body: ["이벤트ID", ...] 1~10개)
	@PostMapping("/hist-drafts")
	public ResponseEntity<?> drafts(@RequestBody List<String> eventIds) {
		try {
			return ResponseEntity.ok(dServ.drafts(eventIds));
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(ex.getMessage());
		}
	}

	// 검수 저장 PUT /api/admin/events/{eventId}/base-text (body: {"baseText": "..."})
	@PutMapping("/events/{eventId}/base-text")
	public ResponseEntity<?> saveBaseText(@PathVariable String eventId, @RequestBody Map<String, String> body) {
		try {
			if (!dServ.saveBaseText(eventId, body.get("baseText"))) {
				return ResponseEntity.status(404).body("없는 역사 이벤트예요.");
			}
			return ResponseEntity.ok(Map.of("eventId", eventId, "isReviewed", "Y"));
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(ex.getMessage());
		}
	}

}