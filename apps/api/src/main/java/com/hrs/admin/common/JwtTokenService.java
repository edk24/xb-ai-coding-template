package com.hrs.admin.common;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import java.time.Instant;
import java.util.Date;

public class JwtTokenService {
    private final Algorithm algorithm;
    private final long expireSeconds;

    public JwtTokenService(String secret, long expireSeconds) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.expireSeconds = expireSeconds;
    }

    public String createToken(Long userId, String username, boolean superAdmin) {
        Instant now = Instant.now();
        return JWT.create()
            .withIssuer("admin-system")
            .withSubject(String.valueOf(userId))
            .withClaim("uid", userId)
            .withClaim("username", username)
            .withClaim("is_super", superAdmin ? 1 : 0)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plusSeconds(expireSeconds)))
            .sign(algorithm);
    }

    public CurrentAdminUser parseToken(String token) {
        try {
            var decoded = JWT.require(algorithm)
                .withIssuer("admin-system")
                .build()
                .verify(token);
            Long userId = decoded.getClaim("uid").asLong();
            String username = decoded.getClaim("username").asString();
            boolean superAdmin = decoded.getClaim("is_super").asInt() == 1;
            return new CurrentAdminUser(userId, username, superAdmin);
        } catch (JWTVerificationException | NullPointerException e) {
            throw new IllegalArgumentException("Token 无效或已过期", e);
        }
    }

    public long expireSeconds() {
        return expireSeconds;
    }
}
