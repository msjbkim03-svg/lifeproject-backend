package com.example.demo.memorial;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MemorialCommentDTO {

	private int commentId;
	private int memberId;
	private String nickname;
	private String content;
	private String createdAt;
	private boolean mine;

	public int getCommentId() { return commentId; }
	public void setCommentId(int commentId) { this.commentId = commentId; }

	@JsonIgnore
	public int getMemberId() { return memberId; }
	public void setMemberId(int memberId) { this.memberId = memberId; }

	public String getNickname() { return nickname; }
	public void setNickname(String nickname) { this.nickname = nickname; }

	public String getContent() { return content; }
	public void setContent(String content) { this.content = content; }

	public String getCreatedAt() { return createdAt; }
	public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

	public boolean isMine() { return mine; }
	public void setMine(boolean mine) { this.mine = mine; }
}
