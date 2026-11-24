package com.example.demo.chat;

import java.util.List;

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
