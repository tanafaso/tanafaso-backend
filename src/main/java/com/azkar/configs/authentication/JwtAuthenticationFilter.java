package com.azkar.configs.authentication;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.azkar.entities.User;
import com.azkar.services.UserService;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  static final String BEARER_TOKEN_PREFIX = "Bearer ";

  @Autowired
  private UserService userService;

  @Value("${app.jwtSecret}")
  private String jwtSecret;

  @Override
  protected void doFilterInternal(
      HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse,
      FilterChain filterChain)
      throws ServletException, IOException {
    String token = extractJwtToken(httpServletRequest);
    if (token != null) {
      JWTVerifier verifier = JWT.require(Algorithm.HMAC512(jwtSecret)).build();
      try {
        String userId = verifier.verify(token).getSubject();
        User currentUser = userService.loadUserById(userId);
        if (currentUser != null) {
          UserPrincipal userPrincipal = new UserPrincipal();
          userPrincipal.setUserId(userId);
          userPrincipal.setUsername(currentUser.getUsername());
          Authentication authToken =
              new PreAuthenticatedAuthenticationToken(
                  userPrincipal, null, userPrincipal.getAuthorities());
          SecurityContextHolder.getContext().setAuthentication(authToken);
        }
      } catch (JWTVerificationException exception) {
        // invalid token.
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
