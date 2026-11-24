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
	
	public String send(String sessionId, String countryCode,ChatDTO dto) {
		Integer memberId = readMemberId(dto.getToken());
		if(memberId == null) {
			return "로그인한 회원만 말할 수 있어요";
		}

		// 이 나라 마을에 들어와 있는(join 할 때 DB로 나라를 확인한) 회원인지
		ChatPlayerDTO me = players.get(sessionId);
		if (me == null || me.getMemberId() != memberId || !countryCode.equals(me.getCountryCode())) {
			return "지금 살고 있는 나라 마을에서만 말할 수 있어요.";
		}

		// 인생이 끝났으면(엔딩 장면 등) 더 말할 수 없음
		if (dao.countMemberLife(memberId, countryCode) == 0) {
			return "지금 살고 있는 인생에서만 말할 수 있어요.";
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
		
		// 금칙어: 띄어쓰기·대소문자를 빼고 비교 ("바 보", "AD" 도 걸림)
		String plain = content.replaceAll("\\s", "").toLowerCase();
		for(String word : dao.selectBannedWords()) {
			String w = word.replaceAll("\\s", "").toLowerCase();
			if (!w.isEmpty() && plain.contains(w)) return "계속해봐 영정 먹여줄게";
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
	
	public boolean join(String sessionId, String countryCode, ChatPlayerDTO dto) {
		ChatPlayerDTO old = players.get(sessionId);
		if (old != null && countryCode.equals(old.getCountryCode())) {
			old.setAge(fixAge(dto.getAge()));
			old.setGender(fixGender(dto.getGender()));
			return true;
		}

		Integer memberId = readMemberId(dto.getToken());
		String nickname;
		if(memberId != null) {
			nickname = dao.selectNickname(memberId);
			if(nickname == null) return false; 
			if(dao.countMemberLife(memberId, countryCode) == 0) return false;
		} else {
			if(dto.getMemberId() >= 0) return false;
			if(dto.getGuestToken() == null || dao.countGuestLife(dto.getGuestToken(), countryCode) == 0) return false;
			memberId = dto.getMemberId();
			nickname="손님";
		}

		dto.setMemberId(memberId);
		dto.setNickname(nickname);
		dto.setCountryCode(countryCode);
		dto.setAge(fixAge(dto.getAge()));
		dto.setGender(fixGender(dto.getGender()));
		dto.setToken(null);
		dto.setGuestToken(null);
		setPosition(dto, dto);

		// 같은 회원이 탭을 여러 개 열면 마지막에 들어온 탭 하나만 남김 (캐릭터가 왔다 갔다 하지 않게)
		if (memberId > 0) {
			final int id = memberId;
			players.values().removeIf(p -> p.getMemberId() == id);
		}
		players.put(sessionId, dto);
		return true;
	}

	// 이 나라 채팅방을 들을 수 있는지 (지금 그 나라에서 살고 있는 회원·비회원만)
	public boolean canListen(String countryCode, String token, String guestToken) {
		Integer memberId = readMemberId(token);
		if (memberId != null) {
			return dao.countMemberLife(memberId, countryCode) > 0;
		}
		return guestToken != null && dao.countGuestLife(guestToken, countryCode) > 0;
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
