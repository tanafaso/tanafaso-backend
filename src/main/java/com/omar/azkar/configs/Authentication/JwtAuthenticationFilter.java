package com.omar.azkar.configs.Authentication;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.spring.security.api.authentication.PreAuthenticatedAuthenticationJsonWebToken;
import com.omar.azkar.entities.User;
import com.omar.azkar.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  String BEARER_TOKEN_PREFIX = "Bearer ";

  @Autowired
  private UserService userService;

  @Value("${app.jwtSecret}")
  private String jwtSecret;

  @Override
  protected void doFilterInternal(HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse, FilterChain filterChain)
      throws ServletException, IOException {
    String token = extractJwtToken(httpServletRequest);
    if (token != null) {
      JWTVerifier verifier = JWT.require(Algorithm.HMAC512(jwtSecret)).build();
      try {
        Authentication authToken = PreAuthenticatedAuthenticationJsonWebToken.usingToken(token)
            .verify(verifier);
        User currentUser = userService
            .loadUserById(authToken.getPrincipal().toString());
        if (currentUser != null) {
          SecurityContextHolder.getContext().setAuthentication(authToken);
        }
      } catch (JWTVerificationException exception) {
        // invalid token
      }
    }
    filterChain.doFilter(httpServletRequest, httpServletResponse);
  }

  private String extractJwtToken(HttpServletRequest httpServletRequest) {
    String token = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
    if (token != null && token.startsWith(BEARER_TOKEN_PREFIX)) {
      return token.replace(BEARER_TOKEN_PREFIX, "");
    }
    return null;
  }
}
