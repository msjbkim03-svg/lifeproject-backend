package com.example.demo.interceptors;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.demo.commons.JwtUtil;
import com.example.demo.member.MemberDAO;
import com.example.demo.member.MemberDTO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// 탈퇴한 회원의 토큰을 막는 문지기 (모든 /api 주소)
// → 토큰이 있으면 DB에서 회원 상태를 확인해서 ACTIVE 가 아니면 401
// ※ 토큰이 없거나(비회원) 잘못된 토큰이면 여기서는 그냥 통과 (원래 처리 그대로)
@Component
public class ActiveMemberValidator implements HandlerInterceptor {

	private JwtUtil jwt;
	private MemberDAO memberDAO;

	public ActiveMemberValidator(JwtUtil jwt, MemberDAO memberDAO) {
		this.jwt = jwt;
		this.memberDAO = memberDAO;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		// 1. CORS 확인 요청은 통과
		if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
			return true;
		}

		// 2. 토큰이 없으면 비회원 → 통과
		String authHeader = request.getHeader("Authorization");
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			return true;
		}

		// 3. 토큰에서 회원 번호 꺼내기 (잘못된 토큰이면 통과 → 각 주소의 원래 처리에 맡김)
		int memberId;
		String pv;
		try {
			DecodedJWT token = jwt.verifyToken(authHeader.substring(7));
			memberId = Integer.parseInt(token.getSubject());
			pv = token.getClaim("pv").asString();
		} catch (Exception e) {
			return true;
		}

		// 4. DB에서 상태 확인 → 없거나 탈퇴했으면 401 (로그인 필요)
		MemberDTO member = memberDAO.selectById(memberId);
		if (member == null || !"ACTIVE".equals(member.getStatus())) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return false;
		}

		// 5. 토큰을 받은 뒤 비밀번호가 바뀌었으면 401 (pv 가 없는 예전 토큰은 그대로 통과)
		if (pv != null && !pv.equals(JwtUtil.passwordVersion(member.getPassword()))) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return false;
		}
		return true;
	}
}
