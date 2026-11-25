package com.example.demo.story;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// C19·C20 AI 점검 (11/25): 브라우저에서 http://localhost/api/ai/ping 을 열면 Gemini 가 되는지 바로 확인
//   { ok, model, ms, kind, message, detail }  ※ 짧은 질문 1번만 보냄, API 키는 가려서 보여 줌
@RestController
public class AiController {

	private final AiCaller ai;

	public AiController(AiCaller ai) {
		this.ai = ai;
	}

	@GetMapping("/api/ai/ping")
	public ResponseEntity<?> ping() {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("model", ai.model());
		long t0 = System.currentTimeMillis();
		try {
			String answer = ai.call("한국어로 아주 짧게 답해.", "'안녕'에 한 단어로 답해.", 45);
			out.put("ok", true);
			out.put("answer", answer.length() > 40 ? answer.substring(0, 40) : answer);
		} catch (AiCaller.AiFail f) {
			out.put("ok", false);
			out.put("kind", f.kind);
			out.put("message", f.userMessage);
			out.put("detail", f.detail);
		}
		out.put("ms", System.currentTimeMillis() - t0);
		out.put("models", ai.status());
		return ResponseEntity.ok(out);
	}
}
