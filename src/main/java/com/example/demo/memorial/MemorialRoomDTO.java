package com.example.demo.memorial;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MemorialRoomDTO {

	private int lifeId;
	private Integer postId;
	private Integer ownerId;
	private String name;
	private int birthYear;
	private int deathYear;
	private int deathAge;
	private String birthCountry;
	private String deathCountry;
	private String deathCause;
	private int childCount;
	private String isMarried;
	private String owns;
	private String summary;
	private String message;
	private int tributeCount;
	private boolean opened;
	private boolean mine;
	private boolean myCandle;
	private List<Integer> happiness;
	private List<String> leftThings;
	private List<MemorialEventDTO> events;
	private List<MemorialEventDTO> photos;
	private List<MemorialCommentDTO> comments;
	private Map<String,Object> last;

	public int getLifeId() { return lifeId; }
	public void setLifeId(int lifeId) { this.lifeId = lifeId; }

	@JsonIgnore
	public Integer getPostId() { return postId; }
	public void setPostId(Integer postId) { this.postId = postId; }

	@JsonIgnore
	public Integer getOwnerId() { return ownerId; }
	public void setOwnerId(Integer ownerId) { this.ownerId = ownerId; }

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public int getBirthYear() { return birthYear; }
	public void setBirthYear(int birthYear) { this.birthYear = birthYear; }

	public int getDeathYear() { return deathYear; }
	public void setDeathYear(int deathYear) { this.deathYear = deathYear; }

	public int getDeathAge() { return deathAge; }
	public void setDeathAge(int deathAge) { this.deathAge = deathAge; }

	public String getBirthCountry() { return birthCountry; }
	public void setBirthCountry(String birthCountry) { this.birthCountry = birthCountry; }

	public String getDeathCountry() { return deathCountry; }
	public void setDeathCountry(String deathCountry) { this.deathCountry = deathCountry; }

	@JsonIgnore
	public String getDeathCause() { return deathCause; }
	public void setDeathCause(String deathCause) { this.deathCause = deathCause; }

	@JsonIgnore
	public int getChildCount() { return childCount; }
	public void setChildCount(int childCount) { this.childCount = childCount; }

	@JsonIgnore
	public String getIsMarried() { return isMarried; }
	public void setIsMarried(String isMarried) { this.isMarried = isMarried; }

	@JsonIgnore
	public String getOwns() { return owns; }
	public void setOwns(String owns) { this.owns = owns; }

	public String getSummary() { return summary; }
	public void setSummary(String summary) { this.summary = summary; }

	public String getMessage() { return message; }
	public void setMessage(String message) { this.message = message; }

	public int getTributeCount() { return tributeCount; }
	public void setTributeCount(int tributeCount) { this.tributeCount = tributeCount; }

	public boolean isOpened() { return opened; }
	public void setOpened(boolean opened) { this.opened = opened; }

	public boolean isMine() { return mine; }
	public void setMine(boolean mine) { this.mine = mine; }

	public boolean isMyCandle() { return myCandle; }
	public void setMyCandle(boolean myCandle) { this.myCandle = myCandle; }

	public List<Integer> getHappiness() { return happiness; }
	public void setHappiness(List<Integer> happiness) { this.happiness = happiness; }

	public List<String> getLeftThings() { return leftThings; }
	public void setLeftThings(List<String> leftThings) { this.leftThings = leftThings; }

	public List<MemorialEventDTO> getEvents() { return events; }
	public void setEvents(List<MemorialEventDTO> events) { this.events = events; }

	public List<MemorialEventDTO> getPhotos() { return photos; }
	public void setPhotos(List<MemorialEventDTO> photos) { this.photos = photos; }

	public List<MemorialCommentDTO> getComments() { return comments; }
	public void setComments(List<MemorialCommentDTO> comments) { this.comments = comments; }

	public Map<String,Object> getLast() { return last; }
	public void setLast(Map<String,Object> last) { this.last = last; }
}
