package com.example.demo.chat;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.example.demo.commons.JwtUtil;

@Service
public class ChatService {
	
	private static final int MAX_TEXT = 100;
	private static final long WAIT_MS = 3000;
	
	private ChatDAO dao;
	private JwtUtil jwt;
	private Map<Integer,Long> lastTime = new ConcurrentHashMap<>();
	private Map<String, ChatPlayerDTO> players = new ConcurrentHashMap<>();
	
	public ChatService(ChatDAO dao,JwtUtil jwt) {
		this.dao=dao;
		this.jwt=jwt;
	}
	
	public String send(String countryCode,ChatDTO dto) {
		Integer memberId = readMemberId(dto.getToken());
		if(memberId == null) {
			return "로그인한 회원만 말할 수 있어요";
		}
		
		String nickname = dao.selectNickname(memberId);
		if(nickname == null) {
			return "로그인한 회원만 말할 수 있어요";
		}
		
		String content = dto.getContent() == null ? "" : dto.getContent().trim();
		if(content.isEmpty()) return "한마디를 적어 주세요.";
		if(content.length() > MAX_TEXT) return "100자까지 쓸 수 있어요.";
		
		long now = System.currentTimeMillis();
		Long last = lastTime.get(memberId);
		if(last != null && now - last < WAIT_MS) return "도배금지 3초 채금";
		
		for(String word : dao.selectBannedWords()) {
			if (content.contains(word)) return "계속해봐 영정 먹여줄게";
		}
		
		lastTime.put(memberId, now);
		
		dto.setCountryCode(countryCode);
		dto.setMemberId(memberId);
		dto.setNickname(nickname);
		dto.setContent(content);
		dto.setToken(null);
		dao.insertChat(dto);
		dto.setCreatedAt(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
		return null;
	}
	
	public List<ChatDTO> recent(String countryCode) {
		return dao.selectRecent(countryCode);
	}
	
	public boolean join(String sessionId, String countryCode, ChatPlayerDTO dto) {
		ChatPlayerDTO old = players.get(sessionId);
		if (old != null && countryCode.equals(old.getCountryCode())) {
			old.setAge(fixAge(dto.getAge()));
			old.setGender(fixGender(dto.getGender()));
			return true;
		}

		Integer memberId = readMemberId(dto.getToken());
		if (memberId == null) return false;
		String nickname = dao.selectNickname(memberId);
		if (nickname == null) return false;

		dto.setMemberId(memberId);
		dto.setNickname(nickname);
		dto.setCountryCode(countryCode);
		dto.setAge(fixAge(dto.getAge()));
		dto.setGender(fixGender(dto.getGender()));
		dto.setToken(null);
		setPosition(dto, dto);
		players.put(sessionId, dto);
		return true;
	}

	public ChatPlayerDTO move(String sessionId, String countryCode, ChatPlayerDTO dto) {
		ChatPlayerDTO me = players.get(sessionId);
		if (me == null || !countryCode.equals(me.getCountryCode())) return null;
		setPosition(me, dto);
		return me;
	}

	public String leave(String sessionId) {
		ChatPlayerDTO old = players.remove(sessionId);
		return old == null ? null : old.getCountryCode();
	}

	public List<ChatPlayerDTO> players(String countryCode) {
		List<ChatPlayerDTO> list = new ArrayList<>();
		for (ChatPlayerDTO p : players.values()) {
			if (countryCode.equals(p.getCountryCode())) list.add(p);
		}
		return list;
	}

	private void setPosition(ChatPlayerDTO me, ChatPlayerDTO pos) {
		String place = pos.getPlace();
		me.setPlace(place == null || place.length() > 20 ? "home" : place);
		me.setX(Math.max(0, Math.min(100, pos.getX())));
		me.setDir("left".equals(pos.getDir()) ? "left" : "right");
		me.setWalking(pos.isWalking());
	}

	private int fixAge(int age) {
		return age < 0 || age > 150 ? 0 : age;
	}

	private String fixGender(String gender) {
		return "F".equals(gender) ? "F" : "M";
	}
	
	private Integer readMemberId(String token) {
		if (token == null) return null;
		try {
			return Integer.parseInt(jwt.verifyToken(token).getSubject());
		} catch (Exception e) {
			return null;
		}
	}
}
