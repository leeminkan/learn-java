package org.leeminkan.bookstore.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.*; // You will need to add a JWT library dependency (like io.jsonwebtoken:jjwt)
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Component
public class JwtCore {
    private final SecretKey jwtSigningKey; // Store the secure key object
    private final int jwtExpirationMs;
    private final String jwtSecret; // Retain the string for initialization

    public JwtCore(@Value("${jwt.secret.key}") String jwtSecret,
                   @Value("${jwt.expiration.ms}") int jwtExpirationMs) {
        this.jwtSecret = jwtSecret;
        this.jwtExpirationMs = jwtExpirationMs;
        // CRITICAL FIX: Create the secure key object ONCE at initialization
        this.jwtSigningKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }


    public String generateToken(String username, List<String> roles) {
        return Jwts.builder().setSubject(username) // Use the username string directly
                .claim("roles", roles) // Add roles to the JWT payload
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                // USE THE STORED KEY OBJECT FOR SIGNING
                .signWith(jwtSigningKey, SignatureAlgorithm.HS512)
                .compact();
    }


    // Method to validate the token's signature and expiration
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(jwtSigningKey)
                    .build() // CRITICAL: Must build the parser before parsing
                    .parseClaimsJws(authToken);
            return true;
        } catch (SignatureException e) {
            // Log that the signature is invalid
            System.err.println("Invalid JWT signature: " + e.getMessage());
        } catch (MalformedJwtException e) {
            // Log malformed JWT (e.g., bad structure)
            System.err.println("Invalid JWT format: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            // Log expired token
            System.err.println("JWT is expired: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            // Log unsupported JWT format
            System.err.println("JWT is unsupported: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            // Log empty token string
            System.err.println("JWT claims string is empty: " + e.getMessage());
        }
        return false;
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder()
                // Use the Keys utility to create a signing key from our secret string
                .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .build() // CRITICAL: This finalizes the parser object
                .parseClaimsJws(token)
                .getBody().getSubject();
    }

}