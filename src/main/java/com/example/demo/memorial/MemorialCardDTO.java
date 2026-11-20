package com.example.demo.memorial;

public class MemorialCardDTO {

	private int lifeId;
	private String name;
	private int birthYear;
	private int deathYear;
	private int deathAge;
	private String birthCountryName;
	private String message;
	private int tributeCount;
	

	public int getLifeId() { return lifeId; }
	public void setLifeId(int lifeId) { this.lifeId = lifeId; }

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public int getBirthYear() { return birthYear; }
	public void setBirthYear(int birthYear) { this.birthYear = birthYear; }

	public int getDeathYear() { return deathYear; }
	public void setDeathYear(int deathYear) { this.deathYear = deathYear; }

	public int getDeathAge() { return deathAge; }
	public void setDeathAge(int deathAge) { this.deathAge = deathAge; }

	public String getBirthCountryName() { return birthCountryName; }
	public void setBirthCountryName(String birthCountryName) { this.birthCountryName = birthCountryName; }

	public String getMessage() { return message; }
	public void setMessage(String message) { this.message = message; }

	public int getTributeCount() { return tributeCount; }
	public void setTributeCount(int tributeCount) { this.tributeCount = tributeCount; }
}
