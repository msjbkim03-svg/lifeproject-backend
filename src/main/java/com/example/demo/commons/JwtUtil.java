package com.example.demo.commons;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

@Component
public class JwtUtil {
	
	private Long expiration;
	private String secret;
	
	private JWTVerifier jwt;
	private Algorithm algo;
	
	public JwtUtil(
			@Value("${jwt.expiration}") Long expiration,
			@Value("${jwt.secret}") String secret) {
		this.expiration = expiration;
		this.secret = secret;
		
		this.algo = Algorithm.HMAC256(secret);
		this.jwt = JWT.require(algo).build();
	}

	public String createToken(String id,String role) {
		return createToken(id, role, null);
	}

	// pv = 비밀번호 버전 (비밀번호가 바뀌면 값이 달라짐 → ActiveMemberValidator 가 예전 토큰을 막음)
	public String createToken(String id, String role, String pv) {
		return JWT.create()
				.withSubject(id)
				.withClaim("role", role)
				.withClaim("pv", pv)
				.withIssuedAt(new Date())
				.withExpiresAt(new Date(System.currentTimeMillis() + expiration))
				.sign(this.algo);
	}
	
	// BCrypt 값으로 짧은 비밀번호 버전 만들기 (해시 원본은 토큰에 안 넣음)
	public static String passwordVersion(String passwordHash) {
		return passwordHash == null ? null : Integer.toHexString(passwordHash.hashCode());
	}

	public DecodedJWT verifyToken(String token) {
		return jwt.verify(token);
	}

	// 토큰 → 회원 번호 (토큰이 없거나 가짜·만료면 null = 비회원으로 봄)
	// 로그인이 선택인 곳(추모관 보기, 코스튬, 채팅)에서 같이 씀
	public Integer memberIdOf(String token) {
		if (token == null || token.isEmpty()) return null;
		try {
			return Integer.parseInt(verifyToken(token).getSubject());
		} catch (Exception e) {
			return null;
		}
	}

	// "Authorization: Bearer 토큰" 헤더 값 → 회원 번호 (없으면 null)
	public Integer memberIdOfHeader(String auth) {
		if (auth == null || !auth.startsWith("Bearer ")) return null;
		return memberIdOf(auth.substring(7));
	}
}
