package com.example.demo.chat;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

// 채팅방 "듣기(구독)" 문지기
// → /topic/chat/{나라} (… /players, /move) 는 지금 그 나라에서 살고 있는 사람만 들을 수 있음
// → 듣기 허락이 안 되면 null 을 돌려줘서 구독을 조용히 버림 (연결은 그대로)
@Component
public class ChatSubscribeGuard implements ChannelInterceptor {

	private ChatService service;

	public ChatSubscribeGuard(ChatService service) {
		this.service = service;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor acc = StompHeaderAccessor.wrap(message);
		if (!StompCommand.SUBSCRIBE.equals(acc.getCommand())) return message;

		String dest = acc.getDestination();
		if (dest == null) return null;

		// 내 안내 우편함(/user/queue/…)은 서버가 내 것으로만 바꿔 주니까 통과
		if (dest.startsWith("/user/")) return message;

		// /queue 를 직접 듣거나, * 로 여러 방을 한꺼번에 들으려 하면 막기
		if (dest.startsWith("/queue") || dest.contains("*")) return null;

		// /topic/chat/{나라}[/players|/move] 만 허락
		String[] parts = dest.split("/");   // ["", "topic", "chat", "KOR", …]
		if (parts.length < 4 || !"topic".equals(parts[1]) || !"chat".equals(parts[2])) return null;

		String token = acc.getFirstNativeHeader("token");
		String guestToken = acc.getFirstNativeHeader("guestToken");
		return service.canListen(parts[3], token, guestToken) ? message : null;
	}
}
