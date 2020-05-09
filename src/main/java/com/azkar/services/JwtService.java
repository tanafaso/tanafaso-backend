package com.azkar.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.azkar.entities.User;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private static final long TOKEN_TIMEOUT_IN_MILLIS = TimeUnit.DAYS.toMillis(7);
  private static final String BEARER_TOKEN_PREFIX = "Bearer ";

  @Value("${app.jwtSecret}")
  String jwtSecret;

  public String generateToken(User user) throws UnsupportedEncodingException {
    String token =
        JWT.create()
            .withSubject(user.getId())
            .withExpiresAt(new Date(System.currentTimeMillis() + TOKEN_TIMEOUT_IN_MILLIS))
            .sign(Algorithm.HMAC512(jwtSecret));
    return token;
  }

  public String extractJwtToken(HttpServletRequest httpServletRequest) {
    String token = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
    if (token != null && token.startsWith(BEARER_TOKEN_PREFIX)) {
      return token.replace(BEARER_TOKEN_PREFIX, "");
    }
    return null;
  }

  public JWTVerifier getVerifier() throws UnsupportedEncodingException {
    return JWT.require(Algorithm.HMAC512(jwtSecret)).build();
  }
}
