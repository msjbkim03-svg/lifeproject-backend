package com.example.demo.chat;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	
	private ChatSubscribeGuard subscribeGuard;
	
	public WebSocketConfig(ChatSubscribeGuard subscribeGuard) {
		this.subscribeGuard = subscribeGuard;
	}
	
	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
	}
	
	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic", "/queue");
		registry.setApplicationDestinationPrefixes("/app");
	}
	
	// 브라우저가 보내는 STOMP 명령(구독 등)을 문지기가 먼저 봄
	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(subscribeGuard);
	}
}
