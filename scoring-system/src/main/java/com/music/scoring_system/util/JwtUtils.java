package com.music.scoring_system.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {
    // 自动生成一个安全的 HMAC-SHA 密钥，存于内存中 (每次重启服务器会刷新，强制所有人重新登录，非常安全)
    private static final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    // Token 有效期：12小时
    private static final long EXPIRE_TIME = 1000 * 60 * 60 * 12;

    public String generateToken(String role) {
        return Jwts.builder()
                .setSubject(role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            // 如果 Token 伪造、被篡改或已过期，这里会直接抛出异常
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}