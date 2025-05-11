package com.molsys.example2.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.molsys.example2.Entity.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long jwtExpirationMs;

    @Value("${jwt.refresh.secret}")
    private String jwtRefreshSecret;

    @Value("${jwt.refresh.expiration}")
    private Long jwtRefreshExpirationMs;

    // Generate a secure SecretKey for access tokens
    private SecretKey getAccessTokenKey() {
        // Use the recommended method to generate a secure key
        // This ensures the key is of proper size (256 bits or more)
        byte[] keyBytes = Base64.getDecoder().decode(getSecureBase64Key(jwtSecret));
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Generate a secure SecretKey for refresh tokens
    private SecretKey getRefreshTokenKey() {
        // Use the recommended method to generate a secure key
        byte[] keyBytes = Base64.getDecoder().decode(getSecureBase64Key(jwtRefreshSecret));
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Ensure the key is properly formatted and of sufficient length
    private String getSecureBase64Key(String originalKey) {
        // If the key is already Base64 and of sufficient length, use it
        // Otherwise, pad it to ensure it's at least 256 bits (32 bytes) when decoded
        try {
            byte[] decodedKey = Base64.getDecoder().decode(originalKey);
            if (decodedKey.length >= 32) {
                return originalKey;
            }
        } catch (IllegalArgumentException e) {
            // Not a valid Base64 string, will create a new one
        }

        // Create a key that's at least 256 bits
        String paddedKey = originalKey;
        while (paddedKey.getBytes(StandardCharsets.UTF_8).length < 32) {
            paddedKey += originalKey;
        }
        return Base64.getEncoder().encodeToString(paddedKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, Role role) {
        return Jwts.builder()
                .claim("id", userId)
                .claim("role", role.name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getAccessTokenKey())
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
                .claim("id", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtRefreshExpirationMs))
                .signWith(getRefreshTokenKey())
                .compact();
    }

    public Jws<Claims> validateAccessToken(String token) {
        return Jwts.parser().setSigningKey(getAccessTokenKey()).parseClaimsJws(token);
    }

    public Jws<Claims> validateRefreshToken(String token) {
        return Jwts.parser().setSigningKey(getRefreshTokenKey()).parseClaimsJws(token);
    }
}