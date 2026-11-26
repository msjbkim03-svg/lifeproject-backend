package com.example.demo.admin;

// A02 관리자 방명록 관리 - 추모관 방명록 댓글 1줄
public class AdminCommentDTO {

	private int commentId;
	private int postId;
	private int lifeId;
	private String lifeName;
	private int memberId;
	private String loginId;
	private String nickname;
	private String content;
	private String createdAt;

	public int getCommentId() { return commentId; }
	public void setCommentId(int commentId) { this.commentId = commentId; }

	public int getPostId() { return postId; }
	public void setPostId(int postId) { this.postId = postId; }

	public int getLifeId() { return lifeId; }
	public void setLifeId(int lifeId) { this.lifeId = lifeId; }

	public String getLifeName() { return lifeName; }
	public void setLifeName(String lifeName) { this.lifeName = lifeName; }

	public int getMemberId() { return memberId; }
	public void setMemberId(int memberId) { this.memberId = memberId; }

	public String getLoginId() { return loginId; }
	public void setLoginId(String loginId) { this.loginId = loginId; }

	public String getNickname() { return nickname; }
	public void setNickname(String nickname) { this.nickname = nickname; }

	public String getContent() { return content; }
	public void setContent(String content) { this.content = content; }

	public String getCreatedAt() { return createdAt; }
	public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
