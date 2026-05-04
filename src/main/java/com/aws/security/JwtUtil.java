
package com.aws.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    private final Key key;

    // Expiry times
    private final long accessTokenExpirationTime = 1000 * 60 * 60 * 24; // 24 hours
    private final long refreshTokenExpirationTime = 1000L * 60 * 60 * 24 * 30; // 30 days

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        if (secret == null || secret.length() < 64) {
            throw new IllegalArgumentException("JWT secret must be at least 64 characters for HS512");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Generate Access Token
    public String generateAccessToken(UserDetails userDetails) {
        List<String> roles = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpirationTime))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    // Generate Refresh Token
    public String generateRefreshToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpirationTime))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    // Extract Username
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    // Validate Access Token
    public boolean validateAccessToken(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException e) {
            System.err.println("Invalid JWT: " + e.getMessage());
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Validate Refresh Token
    public boolean validateRefreshToken(String token) {
        try {
            extractUsername(token);
            return !isTokenExpired(token);
        } catch (JwtException e) {
            System.err.println("Invalid Refresh Token: " + e.getMessage());
            return false;
        }
    }

    // WebSocket Validation
    public boolean validateTokenForWebSocket(String token) {
        try {
            extractUsername(token);
            return !isTokenExpired(token);
        } catch (JwtException e) {
            System.err.println("Invalid WebSocket Token: " + e.getMessage());
            return false;
        }
    }

    // Expiry Check
    private boolean isTokenExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    // Parse Claims
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
 // Extract Expiration
    public Date extractExpiration(String token) {
        return getClaims(token).getExpiration();
    }

    // Remaining Time for Redis TTL
    public long getRemainingTime(String token) {
        Date expiryDate = extractExpiration(token);
        long remaining = expiryDate.getTime() - System.currentTimeMillis();
        return Math.max(remaining, 0);
    }
}
