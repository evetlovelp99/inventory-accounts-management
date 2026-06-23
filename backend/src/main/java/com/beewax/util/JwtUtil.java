package com.beewax.util;

import com.beewax.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

	private final JwtConfig jwtConfig;
	private final SecretKey secretKey;

	public JwtUtil(JwtConfig jwtConfig) {
		this.jwtConfig = jwtConfig;
		this.secretKey = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
	}

	public String generateToken(Long userId, String username, String role) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + jwtConfig.getExpirationMs());

		return Jwts.builder()
				.subject(String.valueOf(userId))
				.claim("username", username)
				.claim("role", role)
				.issuedAt(now)
				.expiration(expiry)
				.signWith(secretKey)
				.compact();
	}

	public Claims parseToken(String token) {
		return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	public boolean isTokenExpired(String token) {
		try {
			parseToken(token);
			return false;
		} catch (ExpiredJwtException e) {
			return true;
		}
	}

	public boolean validateToken(String token) {
		try {
			Claims claims = parseToken(token);
			return !claims.getExpiration().before(new Date());
		} catch (JwtException e) {
			return false;
		}
	}

	public Long getUserId(String token) {
		return Long.parseLong(parseToken(token).getSubject());
	}

	public String getUsername(String token) {
		return parseToken(token).get("username", String.class);
	}

	public String getRole(String token) {
		return parseToken(token).get("role", String.class);
	}
}
