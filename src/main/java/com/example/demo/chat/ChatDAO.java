package com.example.demo.chat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ChatDAO {
	
	private final SqlSessionTemplate mybatis;
	
	public ChatDAO(SqlSessionTemplate mybatis) {
		this.mybatis = mybatis;
	}
	
	public String selectNickname(int memberId) {
		return mybatis.selectOne("chat.selectNickname",memberId);
	}
	
	public int countMemberLife(int memberId, String countryCode) {
		Map<String, Object> map = new HashMap<>();
		map.put("memberId", memberId);
		map.put("countryCode", countryCode);
		return mybatis.selectOne("chat.countMemberLife", map);
	}
	
	public int countGuestLife(String guestToken, String countryCode) {
		Map<String, Object> map = new HashMap<>();
		map.put("guestToken", guestToken);
		map.put("countryCode", countryCode);
		return mybatis.selectOne("chat.countGuestLife", map);
	}
	
	public List<String> selectBannedWords() {
		return mybatis.selectList("chat.selectBannedWords");
	}
	
	public int insertChat(ChatDTO dto) {
		return mybatis.insert("chat.insertChat",dto);
	}
	
	public List<ChatDTO> selectRecent(String countryCode) {
		return mybatis.selectList("chat.selectRecent", countryCode);
	}
}
