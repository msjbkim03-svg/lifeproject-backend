package com.example.demo.chat;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.demo.commons.JwtUtil;
import com.example.demo.member.MemberDAO;
import com.example.demo.member.MemberDTO;

@Service
public class ChatService {
	
	private static final int MAX_TEXT = 50; // 말풍선이라 짧게 (입력칸 maxLength 50 과 같게)
	
	private ChatDAO dao;
	private JwtUtil jwt;
	private MemberDAO memberDAO;
	private Map<String, ChatPlayerDTO> players = new ConcurrentHashMap<>();
	
	public ChatService(ChatDAO dao,JwtUtil jwt, MemberDAO memberDAO) {
		this.dao=dao;
		this.jwt=jwt;
		this.memberDAO=memberDAO;
	}

	// 토큰 → 지금도 쓸 수 있는 회원 번호 (없거나 가짜 · 탈퇴 · 토큰 받은 뒤 비밀번호가 바뀌었으면 null)
	// ※ 웹소켓은 /api 문지기(ActiveMemberValidator)를 안 거쳐서 여기서 같은 검사를 한 번 더 함
	private Integer activeMemberId(String token) {
		if (token == null || token.isEmpty()) return null;
		DecodedJWT t;
		int memberId;
		try {
			t = jwt.verifyToken(token);
			memberId = Integer.parseInt(t.getSubject());
		} catch (Exception e) {
			return null;
		}
		MemberDTO member = memberDAO.selectById(memberId);
		if (member == null || !"ACTIVE".equals(member.getStatus())) return null;
		String pv = t.getClaim("pv").asString();
		if (pv != null && !pv.equals(JwtUtil.passwordVersion(member.getPassword()))) return null;
		return memberId;
	}
	
	public String send(String sessionId, String countryCode,ChatDTO dto) {
		Integer memberId = activeMemberId(dto.getToken());
		if(memberId == null) {
			return "로그인한 회원만 말할 수 있어요.";
		}

		// 이 나라 마을에 들어와 있는(join 할 때 DB로 나라를 확인한) 회원인지
		ChatPlayerDTO me = players.get(sessionId);
		if (me != null && me.getMemberId() < 0 && countryCode.equals(me.getCountryCode())) {
			// 손님으로 남아 있음 = 로그인했지만 지금 하는 인생은 비회원 인생 (회원에게 진행 중인 인생이 이미 있으면 안 옮겨짐)
			return "로그인 전에 시작한 비회원 인생이라 말할 수 없어요.";
		}
		if (me == null || me.getMemberId() != memberId || !countryCode.equals(me.getCountryCode())) {
			return "지금 살고 있는 나라 마을에서만 말할 수 있어요.";
		}

		// 집 안에서는 다른 사람이 안 보여서 말풍선도 못 봄 → 저장도 안 함
		if ("home".equals(me.getPlace())) {
			return "집 안에서는 말할 수 없어요. 밖으로 나가 주세요.";
		}

		// 인생이 끝났으면(엔딩 장면 등) 더 말할 수 없음
		if (dao.countMemberLife(memberId, countryCode) == 0) {
			return "지금 살고 있는 인생에서만 말할 수 있어요.";
		}
		
		String nickname = dao.selectNickname(memberId);
		if(nickname == null) {
			return "로그인한 회원만 말할 수 있어요.";
		}
		
		String content = dto.getContent() == null ? "" : dto.getContent().trim();
		if(content.isEmpty()) return "한마디를 적어 주세요.";
		if(content.length() > MAX_TEXT) return "50자까지 쓸 수 있어요.";
		
		// 금칙어: 띄어쓰기·대소문자를 빼고 비교 ("바 보", "AD" 도 걸림)
		String plain = content.replaceAll("\\s", "").toLowerCase();
		for(String word : dao.selectBannedWords()) {
			String w = word.replaceAll("\\s", "").toLowerCase();
			if (!w.isEmpty() && plain.contains(w)) return "쓸 수 없는 말이 들어 있어요.";
		}
		
		dto.setCountryCode(countryCode);
		dto.setMemberId(memberId);
		dto.setNickname(nickname);
		dto.setContent(content);
		dto.setToken(null);
		dao.insertChat(dto);
		dto.setCreatedAt(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
		return null;
	}
	
	// 마을 입장 → 명단을 다시 보내야 할 나라 코드 목록을 돌려줌 (입장 실패 시 빈 Set)
	public Set<String> join(String sessionId, String countryCode, ChatPlayerDTO dto) {
		Integer memberId = activeMemberId(dto.getToken());

		// 같은 연결 · 같은 나라 · 같은 사람이면 나이 · 성별 · 위치만 바꿈
		// ※ 비회원으로 놀다가 로그인하면 같은 연결인데 사람이 바뀜 → 아래에서 회원으로 다시 들어가게 함
		ChatPlayerDTO old = players.get(sessionId);
		boolean samePerson = old != null
				&& (memberId == null ? old.getMemberId() < 0 : old.getMemberId() == memberId);
		if (samePerson && countryCode.equals(old.getCountryCode())) {
			old.setAge(fixAge(dto.getAge()));
			old.setGender(fixGender(dto.getGender()));
			old.setCostume(dto.getCostume());
			if (dto.getClientId() != null) old.setClientId(dto.getClientId());
			setPosition(old, dto);
			return Set.of(countryCode);
		}

		String nickname;
		if(memberId != null) {
			nickname = dao.selectNickname(memberId);
			if(nickname == null) return Set.of(); 
			if(dao.countMemberLife(memberId, countryCode) == 0) return Set.of();
		} else {
			if(dto.getMemberId() >= 0) return Set.of();
			if(dto.getGuestToken() == null || dao.countGuestLife(dto.getGuestToken(), countryCode) == 0) return Set.of();
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

		Set<String> refresh = new HashSet<>();
		refresh.add(countryCode);
		// 이민 등으로 같은 연결이 다른 나라에서 옮겨 왔으면 예전 나라 명단도 새로 고침
		if (old != null && old.getCountryCode() != null && !countryCode.equals(old.getCountryCode())) {
			refresh.add(old.getCountryCode());
		}
		// 같은 회원이 탭을 여러 개 열면 마지막에 들어온 탭 하나만 남기고, 예전 탭이 있던 나라 명단도 새로 고침
		if (memberId > 0) {
			refresh.addAll(removeOtherSessions(memberId, sessionId));
		}
		players.put(sessionId, dto);
		return refresh;
	}

	// 비회원으로 마을에 들어왔다가 로그인한 경우 → 명단의 손님을 회원(닉네임)으로 바꾸고 갱신할 나라 목록 반환
	// (화면이 다시 join 을 못 보냈어도 말하기 전에 여기서 한 번 더 맞춰 줌)
	public Set<String> upgradeGuest(String sessionId, String countryCode, String token) {
		ChatPlayerDTO me = players.get(sessionId);
		if (me == null || me.getMemberId() >= 0 || !countryCode.equals(me.getCountryCode())) return Set.of();
		Integer memberId = activeMemberId(token);
		if (memberId == null || dao.countMemberLife(memberId, countryCode) == 0) return Set.of();
		String nickname = dao.selectNickname(memberId);
		if (nickname == null) return Set.of();
		Set<String> refresh = removeOtherSessions(memberId, sessionId);
		refresh.add(countryCode);
		me.setMemberId(memberId);
		me.setNickname(nickname);
		return refresh;
	}

	// 같은 회원의 다른 탭 연결을 명단에서 빼고, 그 탭들이 있던 나라 코드 목록을 돌려줌
	private Set<String> removeOtherSessions(int memberId, String keepSessionId) {
		Set<String> countries = new HashSet<>();
		players.entrySet().removeIf(e -> {
			if (e.getValue().getMemberId() == memberId && !e.getKey().equals(keepSessionId)) {
				if (e.getValue().getCountryCode() != null) {
					countries.add(e.getValue().getCountryCode());
				}
				return true;
			}
			return false;
		});
		return countries;
	}

	// 이 나라 채팅방을 들을 수 있는지 (지금 그 나라에서 살고 있는 회원·비회원만)
	public boolean canListen(String countryCode, String token, String guestToken) {
		Integer memberId = activeMemberId(token);
		if (memberId != null && dao.countMemberLife(memberId, countryCode) > 0) {
			return true;
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

	// 브라우저 종료·로그아웃 beacon 요청 시 해당 연결(clientId)만 정확히 접속 해제
	public Set<String> leaveByClientId(String clientId) {
		Set<String> countries = new HashSet<>();
		players.entrySet().removeIf(e -> {
			if (clientId.equals(e.getValue().getClientId())) {
				if (e.getValue().getCountryCode() != null) {
					countries.add(e.getValue().getCountryCode());
				}
				return true;
			}
			return false;
		});
		return countries;
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
	
}
