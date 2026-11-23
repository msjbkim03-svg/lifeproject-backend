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
}
