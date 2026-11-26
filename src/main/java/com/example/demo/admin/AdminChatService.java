package com.example.demo.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// A02 관리자 채팅·방명록·금칙어 관리 - 금칙어 추가·삭제, 마을 채팅·추모관 방명록·추모관 글 조회·삭제
@Service
public class AdminChatService {

	private static final int SIZE = 10;     // 한 페이지에 10줄
	private static final int MAX_WORD = 30; // 금칙어 최대 글자 수

	private final AdminChatDAO dao;

	public AdminChatService(AdminChatDAO dao) {
		this.dao = dao;
	}

	// 검색어 정리 (빈칸은 null, % _ 는 ESCAPE '\' 처리)
	private String cleanKeyword(String keyword) {
		if (keyword == null) return null;
		keyword = keyword.trim();
		if (keyword.isEmpty()) return null;
		return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	// ===== 1. 금칙어 관리 =====
	public List<String> words() {
		return dao.selectWords();
	}

	public String addWord(String word) {
		if (word == null || word.trim().isEmpty()) {
			return "금칙어를 입력해 주세요.";
		}
		String clean = word.trim();
		if (clean.length() > MAX_WORD) {
			return "금칙어는 " + MAX_WORD + "자까지 등록할 수 있어요.";
		}
		if (dao.countWord(clean) > 0) {
			return "이미 등록된 금칙어예요.";
		}
		try {
			dao.insertWord(clean);
		} catch (DuplicateKeyException e) {
			return "이미 등록된 금칙어예요.";
		}
		return null;
	}

	public String removeWord(String word) {
		if (word == null || word.trim().isEmpty()) {
			return "삭제할 금칙어를 골라 주세요.";
		}
		if (dao.deleteWord(word.trim()) == 0) {
			return "이미 삭제되었거나 없는 금칙어예요.";
		}
		return null;
	}

	// ===== 2. 마을 채팅 관리 =====
	public Map<String, Object> chatList(String keyword, String status, int page) {
		if (page < 1) page = 1;
		if (!"N".equals(status) && !"Y".equals(status)) status = null;

		Map<String, Object> cond = new HashMap<>();
		cond.put("keyword", cleanKeyword(keyword));
		cond.put("status", status);
		cond.put("start", (page - 1) * SIZE);
		cond.put("end", page * SIZE);

		Map<String, Object> result = new HashMap<>();
		result.put("total", dao.countChats(cond));
		result.put("page", page);
		result.put("size", SIZE);
		result.put("list", dao.selectChats(cond));
		return result;
	}

	public String removeChat(int msgId) {
		if (dao.deleteChat(msgId) == 0) {
			return "이미 삭제되었거나 없는 채팅이에요.";
		}
		return null;
	}

	// ===== 3. 추모관 방명록 관리 =====
	public Map<String, Object> commentList(String keyword, int page) {
		if (page < 1) page = 1;

		Map<String, Object> cond = new HashMap<>();
		cond.put("keyword", cleanKeyword(keyword));
		cond.put("start", (page - 1) * SIZE);
		cond.put("end", page * SIZE);

		Map<String, Object> result = new HashMap<>();
		result.put("total", dao.countComments(cond));
		result.put("page", page);
		result.put("size", SIZE);
		result.put("list", dao.selectComments(cond));
		return result;
	}

	public String removeComment(int commentId) {
		if (dao.deleteComment(commentId) == 0) {
			return "이미 삭제되었거나 없는 방명록이에요.";
		}
		return null;
	}

	// ===== 4. 추모관 글 관리 (달린 방명록·촛불 기록도 함께 삭제) =====
	public Map<String, Object> postList(String keyword, int page) {
		if (page < 1) page = 1;

		Map<String, Object> cond = new HashMap<>();
		cond.put("keyword", cleanKeyword(keyword));
		cond.put("start", (page - 1) * SIZE);
		cond.put("end", page * SIZE);

		Map<String, Object> result = new HashMap<>();
		result.put("total", dao.countPosts(cond));
		result.put("page", page);
		result.put("size", SIZE);
		result.put("list", dao.selectPosts(cond));
		return result;
	}

	@Transactional
	public String removePost(int postId) {
		dao.deletePostComments(postId);
		dao.deletePostTributes(postId);
		if (dao.deletePost(postId) == 0) {
			return "이미 삭제되었거나 없는 추모관 글이에요.";
		}
		return null;
	}
}
