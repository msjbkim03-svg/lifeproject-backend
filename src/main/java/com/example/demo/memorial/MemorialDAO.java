package com.example.demo.memorial;

import java.util.List;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MemorialDAO {

	private final SqlSessionTemplate mybatis;

	public MemorialDAO(SqlSessionTemplate mybatis) {
		this.mybatis = mybatis;
	}

	public List<MemorialCardDTO> selectCards(int start, int end) {
		return mybatis.selectList("memorial.selectCards", Map.of("start", start, "end", end));
	}

	public int countCards() {
		return mybatis.selectOne("memorial.countCards");
	}

	public MemorialRoomDTO selectRoom(int lifeId) {
		return mybatis.selectOne("memorial.selectRoom", lifeId);
	}

	public MemorialJobDTO selectMainJob(int lifeId) {
		return mybatis.selectOne("memorial.selectMainJob", lifeId);
	}

	public List<Integer> selectHappiness(int lifeId) {
		return mybatis.selectList("memorial.selectHappiness", lifeId);
	}

	public List<MemorialEventDTO> selectHistEvents(int lifeId) {
		return mybatis.selectList("memorial.selectHistEvents", lifeId);
	}

	public List<MemorialEventDTO> selectLogs(int lifeId) {
		return mybatis.selectList("memorial.selectLogs", lifeId);
	}

	public int countCountries(int lifeId) {
		return mybatis.selectOne("memorial.countCountries", lifeId);
	}

	public int countAchievedQuests(int lifeId) {
		return mybatis.selectOne("memorial.countAchievedQuests", lifeId);
	}

	public List<MemorialCommentDTO> selectComments(int postId) {
		return mybatis.selectList("memorial.selectComments", postId);
	}

	public int countMyCandle(int postId, int memberId) {
		return mybatis.selectOne("memorial.countMyCandle", Map.of("postId", postId, "memberId", memberId));
	}

	public int insertPost(int lifeId, int memberId, String message) {
		return mybatis.insert("memorial.insertPost", Map.of("lifeId", lifeId, "memberId", memberId, "message", message));
	}

	public int insertTribute(int postId, int memberId) {
		return mybatis.insert("memorial.insertTribute", Map.of("postId", postId, "memberId", memberId));
	}

	public int plusTributeCount(int postId) {
		return mybatis.update("memorial.plusTributeCount", postId);
	}

	public int plusViewCount(int postId) {
		return mybatis.update("memorial.plusViewCount", postId);
	}

	public int insertComment(int postId, int memberId, String content) {
		return mybatis.insert("memorial.insertComment", Map.of("postId", postId, "memberId", memberId, "content", content));
	}

	public int deleteComment(int commentId, int memberId) {
		return mybatis.delete("memorial.deleteComment", Map.of("commentId", commentId, "memberId", memberId));
	}
}
