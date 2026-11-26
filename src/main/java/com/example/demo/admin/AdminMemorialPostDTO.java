package com.example.demo.admin;

// A02 관리자 추모관 글 관리 - 열린 추모관 1줄
public class AdminMemorialPostDTO {

	private int postId;
	private int lifeId;
	private String lifeName;
	private String countryName;
	private int memberId;
	private String loginId;
	private String nickname;
	private String message;
	private int tributeCount;
	private int viewCount;
	private int commentCount;
	private String createdAt;

	public int getPostId() { return postId; }
	public void setPostId(int postId) { this.postId = postId; }

	public int getLifeId() { return lifeId; }
	public void setLifeId(int lifeId) { this.lifeId = lifeId; }

	public String getLifeName() { return lifeName; }
	public void setLifeName(String lifeName) { this.lifeName = lifeName; }

	public String getCountryName() { return countryName; }
	public void setCountryName(String countryName) { this.countryName = countryName; }

	public int getMemberId() { return memberId; }
	public void setMemberId(int memberId) { this.memberId = memberId; }

	public String getLoginId() { return loginId; }
	public void setLoginId(String loginId) { this.loginId = loginId; }

	public String getNickname() { return nickname; }
	public void setNickname(String nickname) { this.nickname = nickname; }

	public String getMessage() { return message; }
	public void setMessage(String message) { this.message = message; }

	public int getTributeCount() { return tributeCount; }
	public void setTributeCount(int tributeCount) { this.tributeCount = tributeCount; }

	public int getViewCount() { return viewCount; }
	public void setViewCount(int viewCount) { this.viewCount = viewCount; }

	public int getCommentCount() { return commentCount; }
	public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

	public String getCreatedAt() { return createdAt; }
	public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
