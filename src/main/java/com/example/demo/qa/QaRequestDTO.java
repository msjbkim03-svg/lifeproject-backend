package com.example.demo.qa;

// C20 질문 보내기 요청 { "question": "...", "guestToken": "...", "sinceQaId": 123 }
public class QaRequestDTO {
	private String question;	// 질문
	private String guestToken;	// 비회원 임시 토큰 (회원은 null)
	private Integer sinceQaId;	// 이번 대화의 첫 질문 번호 (이 번호부터의 대화만 AI가 참고, null = 새 대화) 11/25

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public String getGuestToken() {
		return guestToken;
	}

	public void setGuestToken(String guestToken) {
		this.guestToken = guestToken;
	}

	public Integer getSinceQaId() {
		return sinceQaId;
	}

	public void setSinceQaId(Integer sinceQaId) {
		this.sinceQaId = sinceQaId;
	}
}
