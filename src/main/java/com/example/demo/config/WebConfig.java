package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.demo.interceptors.ActiveMemberValidator;
import com.example.demo.interceptors.AdminValidator;
import com.example.demo.interceptors.LoginValidator;

// LoginValidator(문지기)를 어느 주소에 세울지 정하는 곳
// ※ 비회원도 쓰는 주소(engine, event 등)는 넣지 않음 → 로그인 없이도 게임 가능
@Configuration
public class WebConfig implements WebMvcConfigurer {

	private LoginValidator loginValidator;
	private ActiveMemberValidator activeMemberValidator;
	private AdminValidator adminValidator;

	public WebConfig(LoginValidator loginValidator, ActiveMemberValidator activeMemberValidator,
			AdminValidator adminValidator) {
		this.loginValidator = loginValidator;
		this.activeMemberValidator = activeMemberValidator;
		this.adminValidator = adminValidator;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(loginValidator)
				// 회원 전용 주소 (마이페이지·정보 수정·탈퇴·특성 강화 등 /api/members/... 아래)
				.addPathPatterns("/api/members/**", "/api/quests/**", // 퀘스트(C01)도 회원만
						"/api/admin/**", // 관리자 주소 (관리자 확인은 AdminValidator)
						"/api/memorials/*/open", "/api/memorials/*/candle", // 추모관(B01) 열기·촛불·방명록
						"/api/memorials/*/comments", "/api/memorials/comments/**")
				// 로그인 없이 써야 하는 주소는 빼기
				.excludePathPatterns("/api/members", // 회원가입
						"/api/members/login", // 로그인
						"/api/members/google/**", // 구글 소셜 로그인·가입
						"/api/members/check*", // 아이디·닉네임·이메일 중복 확인
						"/api/members/find/**"); // 아이디·비번 찾기

		// 탈퇴한 회원의 토큰 막기 (모든 /api 주소, 게임 API 포함)
		// 토큰이 없으면(비회원) 그냥 통과 → 비회원 게임은 그대로 됨
		registry.addInterceptor(activeMemberValidator).addPathPatterns("/api/**")
				// 로그인·가입·중복확인·아이디/비번 찾기는 빼기
				// → 탈퇴한 토큰이 브라우저에 남아 있어도 다시 로그인·가입할 수 있게
				.excludePathPatterns("/api/members", // 회원가입
						"/api/members/login", // 로그인
						"/api/members/google/**", // 구글 소셜 로그인·가입
						"/api/members/check*", // 중복 확인
						"/api/members/find/**"); // 아이디·비번 찾기

		// 관리자 전용 주소 (A01 회원 관리, A03 이벤트 관리, A04 관리자 메인)
		// LoginValidator 다음에 실행돼야 함 → 맨 아래에 등록
		registry.addInterceptor(adminValidator).addPathPatterns("/api/admin/**");
	}
}
