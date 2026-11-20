package com.example.demo.interceptors;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.demo.admin.AdminService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// 관리자 주소(/api/admin/**) 문지기 - 관리자(ADMIN)가 아니면 403
// 토큰 검사는 앞에서 LoginValidator 가 이미 함 (없거나 잘못되면 401, 맞으면 memberId 담아 둠)
// → 여기서는 담아 둔 memberId 로 관리자인지만 확인 (A04 AdminService.isAdmin 사용)
@Component
public class AdminValidator implements HandlerInterceptor {

	private AdminService adminService;

	public AdminValidator(AdminService adminService) {
		this.adminService = adminService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		// 1. CORS 확인 요청은 통과
		if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
			return true;
		}

		// 2. LoginValidator 가 담아 둔 회원 번호 (String)
		String memberId = (String) request.getAttribute("memberId");

		// 3. 관리자(ADMIN이고 ACTIVE)가 아니면 403
		if (memberId == null || !adminService.isAdmin(Integer.parseInt(memberId))) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return false;
		}
		return true;
	}
}