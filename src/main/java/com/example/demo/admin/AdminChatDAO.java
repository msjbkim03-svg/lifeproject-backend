package com.example.demo.admin;

import java.util.List;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

// A02 관리자 채팅·방명록·금칙어 관리 DB 접근 (SQL 은 resources/mappers/admin-chat-mapper.xml, namespace "adminChat")
@Repository
public class AdminChatDAO {

	private final SqlSessionTemplate mybatis;

	public AdminChatDAO(SqlSessionTemplate mybatis) {
		this.mybatis = mybatis;
	}

	// ===== 금칙어 (BANNED_WORD) =====
	public List<String> selectWords() {
		return mybatis.selectList("adminChat.selectWords");
	}

	public int countWord(String word) {
		return mybatis.selectOne("adminChat.countWord", word);
	}

	public int insertWord(String word) {
		return mybatis.insert("adminChat.insertWord", word);
	}

	public int deleteWord(String word) {
		return mybatis.delete("adminChat.deleteWord", word);
	}

	// ===== 마을 채팅 (CHAT_MESSAGE) =====
	public List<AdminChatDTO> selectChats(Map<String, Object> cond) {
		return mybatis.selectList("adminChat.selectChats", cond);
	}

	public int countChats(Map<String, Object> cond) {
		return mybatis.selectOne("adminChat.countChats", cond);
	}

	public int deleteChat(int msgId) {
		return mybatis.update("adminChat.deleteChat", msgId);
	}

	// ===== 추모관 방명록 (MEMORIAL_COMMENT) =====
	public List<AdminCommentDTO> selectComments(Map<String, Object> cond) {
		return mybatis.selectList("adminChat.selectComments", cond);
	}

	public int countComments(Map<String, Object> cond) {
		return mybatis.selectOne("adminChat.countComments", cond);
	}

	public int deleteComment(int commentId) {
		return mybatis.delete("adminChat.deleteComment", commentId);
	}

	// ===== 추모관 글 (MEMORIAL_POST) =====
	public List<AdminMemorialPostDTO> selectPosts(Map<String, Object> cond) {
		return mybatis.selectList("adminChat.selectPosts", cond);
	}

	public int countPosts(Map<String, Object> cond) {
		return mybatis.selectOne("adminChat.countPosts", cond);
	}

	public int deletePostComments(int postId) {
		return mybatis.delete("adminChat.deletePostComments", postId);
	}

	public int deletePostTributes(int postId) {
		return mybatis.delete("adminChat.deletePostTributes", postId);
	}

	public int deletePost(int postId) {
		return mybatis.delete("adminChat.deletePost", postId);
	}
}
