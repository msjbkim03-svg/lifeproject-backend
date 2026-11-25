package com.example.demo.chat;

import java.util.List;
import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
	
	private ChatService service;
	private SimpMessagingTemplate template;
	
	public ChatController(ChatService service, SimpMessagingTemplate template) {
		this.service = service;
		this.template = template;
	}
	
	@MessageMapping("/chat/{countryCode}")
	@SendToUser("/queue/chatError")
	public Map<String,String> send(@DestinationVariable String countryCode, @Header("simpSessionId") String sessionId, ChatDTO dto) {
		if (countryCode == null || countryCode.length() != 3) {
			return Map.of("message", "나라를 찾지 못했어요.");
		}
		// 비회원으로 들어왔다가 로그인했으면 먼저 회원으로 바꾸고, 영향받은 나라의 명단도 새로 보냄
		for (String code : service.upgradeGuest(sessionId, countryCode, dto.getToken())) {
			sendPlayers(code);
		}
		String error = service.send(sessionId, countryCode, dto);
		if(error != null) {
			return Map.of("message", error);
		}
		template.convertAndSend("/topic/chat/" + countryCode, dto);
		return null;
	}
	
	@GetMapping("/{countryCode}/players")
	public List<ChatPlayerDTO> players(@PathVariable String countryCode) {
		return service.players(countryCode);
	}

	@MessageMapping("/chat/{countryCode}/join")
	public void join(@DestinationVariable String countryCode, @Header("simpSessionId") String sessionId, ChatPlayerDTO dto) {
		if (countryCode.length() != 3) return;
		for (String code : service.join(sessionId, countryCode, dto)) {
			sendPlayers(code);
		}
	}

	@MessageMapping("/chat/{countryCode}/move")
	public void move(@DestinationVariable String countryCode, @Header("simpSessionId") String sessionId, ChatPlayerDTO dto) {
		ChatPlayerDTO me = service.move(sessionId, countryCode, dto);
		if (me != null) {
			template.convertAndSend("/topic/chat/" + countryCode + "/move", me);
		}
	}

	@EventListener
	public void leave(SessionDisconnectEvent event) {
		String countryCode = service.leave(event.getSessionId());
		if (countryCode != null) {
			sendPlayers(countryCode);
		}
	}

	private void sendPlayers(String countryCode) {
		template.convertAndSend("/topic/chat/" + countryCode + "/players", service.players(countryCode));
	}
}
