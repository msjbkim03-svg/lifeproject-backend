package com.example.demo.admin;

// A04 수명 분포 (10년 단위, 끝까지 산 인생 - 지워진 비회원 포함)
public class AgeBucketDTO {
	private int fromAge;	// 구간 시작 나이 (0, 10, 20 …)
	private int lives;	// 그 구간에서 떠난 인생 수

	public int getFromAge() {
		return fromAge;
	}

	public void setFromAge(int fromAge) {
		this.fromAge = fromAge;
	}

	public int getLives() {
		return lives;
	}

	public void setLives(int lives) {
		this.lives = lives;
	}
}
