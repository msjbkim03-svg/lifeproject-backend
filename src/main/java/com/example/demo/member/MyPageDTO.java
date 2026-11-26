package com.example.demo.member;

// 마이페이지(M04) 화면에 보낼 정보를 담는 상자
// member-mapper.xml 의 selectMyPage 결과가 여기에 담긴다
public class MyPageDTO {

	// ===== 가입 정보 (MEMBER) =====
	private String loginId;     // 아이디
	private String nickname;    // 닉네임
	private String email;       // 이메일
	private String phone;       // 전화번호 (숫자만, 없으면 null - 예전 가입 회원)
	private String joinDate;    // 가입 일자 (SQL에서 'YYYY-MM-DD' 글자로 바꿔서 받음)
	private int point;          // 보유 포인트

	// ===== 숫자 칸 3개 =====
	private int lifeCount;      // 살아본 인생 수 (끝난 인생)
	private int questDone;      // 퀘스트 달성 수

	// ===== 진행 중인 인생 (없으면 전부 null) =====
	private Integer lifeId;     // 인생 번호 (int 는 null 을 못 담아서 Integer)
	private String lifeName;    // 캐릭터 이름
	private String currentCountryName; // 지금 사는 나라
	private Integer currentAge; // 지금 나이
	private String gender;      // 캐릭터 성별
	private String hair;        // 착용 머리 코드
	private String top;         // 착용 옷 코드
	private String hat;         // 착용 모자 코드

	public String getLoginId() { return loginId; }
	public void setLoginId(String loginId) { this.loginId = loginId; }

	public String getNickname() { return nickname; }
	public void setNickname(String nickname) { this.nickname = nickname; }

	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	public String getPhone() { return phone; }
	public void setPhone(String phone) { this.phone = phone; }

	public String getJoinDate() { return joinDate; }
	public void setJoinDate(String joinDate) { this.joinDate = joinDate; }

	public int getPoint() { return point; }
	public void setPoint(int point) { this.point = point; }

	public int getLifeCount() { return lifeCount; }
	public void setLifeCount(int lifeCount) { this.lifeCount = lifeCount; }

	public int getQuestDone() { return questDone; }
	public void setQuestDone(int questDone) { this.questDone = questDone; }

	public Integer getLifeId() { return lifeId; }
	public void setLifeId(Integer lifeId) { this.lifeId = lifeId; }

	public String getLifeName() { return lifeName; }
	public void setLifeName(String lifeName) { this.lifeName = lifeName; }

	public String getCurrentCountryName() { return currentCountryName; }
	public void setCurrentCountryName(String currentCountryName) { this.currentCountryName = currentCountryName; }

	public Integer getCurrentAge() { return currentAge; }
	public void setCurrentAge(Integer currentAge) { this.currentAge = currentAge; }

	public String getGender() { return gender; }
	public void setGender(String gender) { this.gender = gender; }

	public String getHair() { return hair; }
	public void setHair(String hair) { this.hair = hair; }

	public String getTop() { return top; }
	public void setTop(String top) { this.top = top; }

	public String getHat() { return hat; }
	public void setHat(String hat) { this.hat = hat; }
}
