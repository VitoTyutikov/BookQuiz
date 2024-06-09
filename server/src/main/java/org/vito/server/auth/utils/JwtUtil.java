package org.vito.server.auth.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtUtil {
    SecretKey key = Jwts.SIG.HS512.key().build(); // or HS384.key() or HS512.key()

    public String extractUsername(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (Exception e) {
            return null;
        }
    }

    public Date extractExpiration(String token) {
        try {
            return extractClaim(token, Claims::getExpiration);
        } catch (Exception e) {
            return null;
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {

        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Map<String, String> generateTokens(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        String accessToken = createToken(claims, userDetails.getUsername(), Duration.ofMinutes(15L), "ACCESS");
        String refreshToken = createToken(claims, userDetails.getUsername(), Duration.ofDays(30L), "REFRESH");

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        return tokens;
    }

    private String createToken(Map<String, Object> claims, String subject, Duration duration, String tokenType) {
        claims.put("token_type", tokenType);
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + duration.toMillis()))
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token, UserDetails userDetails, String expectedTokenType) {
        try {
            final String username = extractUsername(token);
            final String tokenType = extractClaim(token, claims -> claims.get("token_type", String.class));
            return (username.equals(userDetails.getUsername())
                    && !isTokenExpired(token)
                    && expectedTokenType.equals(tokenType));
        } catch (Exception e) {
            return false;
        }
    }
}
