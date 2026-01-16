package com.guru2.memody.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;

@Component
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String SECRET_KEY;
    private final long EXPIRATION_TIME = 3600 * 1000L; //토큰유지 1시간

    public String generateJwtToken(String email) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + EXPIRATION_TIME);

        String token = Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()), SignatureAlgorithm.HS256)
                .compact();
        return token;
    }

    public String getEmailFromJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateJwtToken(String token) {
        try {
            System.out.println("[VALIDATE TOKEN] " + token);

            // 실제 검증
            Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY.getBytes())
                    .build()
                    .parseClaimsJws(token);

            System.out.println("[JWT VALID] Token is valid.");
            return true;

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            System.out.println("[JWT ERROR] Token expired: " + e.getMessage());
        } catch (io.jsonwebtoken.SignatureException e) {
            System.out.println("[JWT ERROR] Invalid signature: " + e.getMessage());
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            System.out.println("[JWT ERROR] Malformed token: " + e.getMessage());
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            System.out.println("[JWT ERROR] Unsupported token: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("[JWT ERROR] Empty or null token: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[JWT ERROR] Unknown error: " + e.getMessage());
        }

        System.out.println("[JWT INVALID] Token validation failed.");
        return false;
    }

}