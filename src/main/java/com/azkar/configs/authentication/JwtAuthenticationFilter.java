package com.azkar.configs.authentication;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.azkar.configs.SecurityConfig;
import com.azkar.entities.User;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.authenticationcontroller.responses.UnauthenticatedResponse;
import com.azkar.services.JwtService;
import com.azkar.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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
    // TODO(issue#111): Find a better way to not run JwtAuthenticationFilter for some URLs.
    if (Arrays.stream(SecurityConfig.PRE_AUTHENTICAITON_ALLOWED_ENDPOINTS).filter(
        (preAuthenticationAllowedEndpoint) -> preAuthenticationAllowedEndpoint
            .equals(httpServletRequest.getRequestURI())).findAny().isPresent()) {
      logger.info("No JWT authentication needed.");

      filterChain.doFilter(httpServletRequest, httpServletResponse);
      return;
    }

    String token = jwtService.extractJwtToken(httpServletRequest);
    logger.info("Authenticating a new request.");

    if (token == null) {
      logger.info("No token found.");

      setUnAuthenticatedResponse(httpServletResponse);
      return;
    }

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

      filterChain.doFilter(httpServletRequest, httpServletResponse);
    } catch (JWTVerificationException exception) {
      logger.info("Token used is invalid.");

      setUnAuthenticatedResponse(httpServletResponse);
    }
  }

  private void setUnAuthenticatedResponse(HttpServletResponse httpServletResponse)
      throws IOException {
    httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);

    UnauthenticatedResponse unauthenticatedResponse = new UnauthenticatedResponse();
    unauthenticatedResponse.setError(new Error(Error.AUTHENTICATION_ERROR));
    ObjectMapper objectMapper = new ObjectMapper();
    String responseBody = objectMapper.writeValueAsString(unauthenticatedResponse);
    httpServletResponse.getWriter().write(responseBody);
  }

}
