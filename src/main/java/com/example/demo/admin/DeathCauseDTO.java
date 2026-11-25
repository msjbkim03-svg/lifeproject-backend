package com.example.demo.admin;

// A04 사망 원인 분포 (끝까지 산 인생, LIFE 에 남은 것만 - 지워진 비회원은 원인 기록이 없음)
public class DeathCauseDTO {
	private String cause;	// 사망 원인 (C07 분류 이름)
	private int lives;	// 그 원인으로 떠난 인생 수
	private double avgAge;	// 평균 수명

	public String getCause() {
		return cause;
	}

	public void setCause(String cause) {
		this.cause = cause;
	}

	public int getLives() {
		return lives;
	}

	public void setLives(int lives) {
		this.lives = lives;
	}

	public double getAvgAge() {
		return avgAge;
	}

	public void setAvgAge(double avgAge) {
		this.avgAge = avgAge;
	}
}
