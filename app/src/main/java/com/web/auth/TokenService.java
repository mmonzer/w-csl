package com.csl.web.auth;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.csl.web.auth.user.Role;
import com.csl.web.auth.user.User;
import com.csl.web.auth.user.UserPrincipal;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;

public final class TokenService {

    private static final long EXPIRATION_TIME = 60 * 60 * 1000l; // 60 minutes
    private static final String ROLES = "roles";

    private final String jwtSecretKey;

    private final BlacklistedTokenRepository blacklistedTokenRepository = new BlacklistedTokenRepository();

    public TokenService(String jwtSecretKey) {
    	System.out.println("TokenService:"+jwtSecretKey);
        this.jwtSecretKey = jwtSecretKey;
    }

    public final void removeExpired() {
        blacklistedTokenRepository.removeExpired();
    }

    public final String newToken(User user) {
    	System.out.println("newtoken");
    
    	try {
        DefaultClaims claims = new DefaultClaims();
        claims.put(ROLES, user.getRoles().toString());
        claims.setSubject(user.getUsername());
        JwtBuilder builder = Jwts.builder();
        System.out.println("builder="+builder);
        
        SignatureAlgorithm sa = SignatureAlgorithm.HS512;
        String z = Jwts.builder()
        		.serializeToJsonWith(new CSLSerializer())
                .setClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS512, jwtSecretKey)
                .compact();
        System.out.println("token :"+z);
        return z;
    	} catch (Exception e) {
    		System.out.println(e);
    	}
        return "token";
    }

    public final void revokeToken(String token) {
        Date expirationDate = Jwts.parser()
                .setSigningKey(jwtSecretKey)
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        blacklistedTokenRepository.addToken(token, expirationDate.getTime());
    }

    /**
     * throws ExpiredJwtException if token has expired
     *
     * @param token
     * @return
     */
    public final UserPrincipal getUserPrincipal(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecretKey)
                .parseClaimsJws(token)
                .getBody();
        List<String> roles = (List<String>) claims.get(ROLES);
        return UserPrincipal.of(claims.getSubject(), roles.stream().map(role -> Role.valueOf(role)).collect(Collectors.toList()));
    }

    public final boolean isTokenBlacklisted(String token) {
        return blacklistedTokenRepository.isTokenBlacklisted(token);
    }

    public final boolean validateToken(String token) {
    	System.out.println("VALIDATE TOKEN : +token");
        if (!isTokenBlacklisted(token)) {
            try {
                getUserPrincipal(token);
                return true;
            } catch (Exception e) {
                return false;
            }
        } else {
            return false;
        }
    }

}
