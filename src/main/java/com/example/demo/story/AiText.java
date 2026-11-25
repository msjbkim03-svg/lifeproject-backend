package com.example.demo.story;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

// C19·C20 공통: Gemini 응답을 끝까지 이어 붙여서 글자로 만들기
// Gemini는 답을 여러 조각(part)으로 나눠 보낼 때가 있는데, .content()는 첫 조각만 돌려줘서 글이 중간에 끊김
public class AiText {

	public static String join(ChatResponse response) {
		if (response == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for (Generation g : response.getResults()) {
			if (g.getOutput() != null && g.getOutput().getText() != null) {
				sb.append(g.getOutput().getText());
			}
			// 길이 제한 등으로 중간에 멈췄으면 콘솔에 이유 표시 (정상 종료는 STOP)
			String reason = g.getMetadata() == null ? null : g.getMetadata().getFinishReason();
			if (reason != null && !"STOP".equalsIgnoreCase(reason)) {
				System.out.println("[AI] 답변이 끝까지 오지 않았어요. finishReason=" + reason);
			}
		}
		return sb.toString();
	}

	// AI 글 다듬기 (C19 · C20 공통, 11/25 점검)
	//  - '[2, 3]' · '[2,3]' → '[2][3]' (화면은 [번호] 하나씩만 작은 글자로 바꿈)
	//  - 규칙으로 막아도 자꾸 나오는 뻔한 꾸밈말 빼기 ('묵묵히', '굴곡진' - 빼도 문장이 성립하는 말만)
	//  - **굵게** 표시 빼기, 빈 줄 정리
	public static String tidy(String s) {
		if (s == null) {
			return "";
		}
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[(\\d+(?:\\s*,\\s*\\d+)+)\\]").matcher(s);
		StringBuilder sb = new StringBuilder();
		while (m.find()) {
			StringBuilder rep = new StringBuilder();
			for (String n : m.group(1).split(",")) {
				rep.append("[").append(n.trim()).append("]");
			}
			m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(rep.toString()));
		}
		m.appendTail(sb);
		return sb.toString()
				.replace("**", "")
				.replace("묵묵히 ", "")
				.replace("굴곡진 ", "")
				.replaceAll(" +\\[", "[")
				.replaceAll("\\n{3,}", "\n\n")
				.trim();
	}
}
