package com.azkar.configs.authentication;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.azkar.entities.User;
import com.azkar.services.JwtService;
import com.azkar.services.UserService;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  @Autowired
  JwtService jwtService;
  @Autowired
  private UserService userService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse,
      FilterChain filterChain)
      throws ServletException, IOException {
    String token = jwtService.extractJwtToken(httpServletRequest);
    logger.info("Authenticating a new request.");

    if (token != null) {
      logger.info(String.format("Token used for authentication is: %s", token));

      try {
        String userId = jwtService.getVerifier().verify(token).getSubject();
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
        logger.info("Token used is invalid.");
      }
    }
    filterChain.doFilter(httpServletRequest, httpServletResponse);
  }

}
