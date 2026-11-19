package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.demo.interceptors.ActiveMemberValidator;
import com.example.demo.interceptors.LoginValidator;

// LoginValidator(문지기)를 어느 주소에 세울지 정하는 곳
// ※ 비회원도 쓰는 주소(engine, event 등)는 넣지 않음 → 로그인 없이도 게임 가능
@Configuration
public class WebConfig implements WebMvcConfigurer {

	private LoginValidator loginValidator;
	private ActiveMemberValidator activeMemberValidator;

	public WebConfig(LoginValidator loginValidator, ActiveMemberValidator activeMemberValidator) {
		this.loginValidator = loginValidator;
		this.activeMemberValidator = activeMemberValidator;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(loginValidator)
				// 회원 전용 주소 (마이페이지·정보 수정·탈퇴·특성 강화 등 /api/members/... 아래)
				.addPathPatterns("/api/members/**", "/api/quests/**") // 퀘스트(C01)도 회원만
				// 로그인 없이 써야 하는 주소는 빼기
				.excludePathPatterns(
						"/api/members",          // 회원가입
						"/api/members/login",    // 로그인
						"/api/members/oauth/**", // 소셜 로그인 (카카오)
						"/api/members/check*",   // 아이디·닉네임·이메일 중복 확인
						"/api/members/find/**"); // 아이디·비번 찾기

		// 탈퇴한 회원의 토큰 막기 (모든 /api 주소, 게임 API 포함)
		// 토큰이 없으면(비회원) 그냥 통과 → 비회원 게임은 그대로 됨
		registry.addInterceptor(activeMemberValidator)
				.addPathPatterns("/api/**")
				// 로그인·가입·중복확인·아이디/비번 찾기는 빼기
				// → 탈퇴한 토큰이 브라우저에 남아 있어도 다시 로그인·가입할 수 있게
				.excludePathPatterns(
						"/api/members",          // 회원가입
						"/api/members/login",    // 로그인
						"/api/members/check*",   // 중복 확인
						"/api/members/oauth/**", // 소셜 로그인
						"/api/members/find/**"); // 아이디·비번 찾기
	}
}
