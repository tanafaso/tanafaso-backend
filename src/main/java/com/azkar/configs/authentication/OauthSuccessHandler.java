package com.azkar.configs.authentication;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.azkar.entities.User;
import com.azkar.repos.UserRepo;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OauthSuccessHandler implements AuthenticationSuccessHandler {

  private static long TOKEN_TIMEOUT_IN_MILLIS = TimeUnit.DAYS.toMillis(7);

  @Autowired
  UserRepo userRepo;

  @Value("${app.jwtSecret}")
  String jwtSecret;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse,
      Authentication authentication)
      throws IOException, ServletException {
    String email = ((DefaultOAuth2User) authentication.getPrincipal()).getAttribute("email");
    String name = ((DefaultOAuth2User) authentication.getPrincipal()).getAttribute("name");
    User user = User.builder().email(email).name(name).build();
    String token = generateToken(user);
    httpServletResponse.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
  }

  public String generateToken(User user) throws UnsupportedEncodingException {
    String token =
        JWT.create()
            .withSubject(user.getId())
            .withExpiresAt(new Date(System.currentTimeMillis() + TOKEN_TIMEOUT_IN_MILLIS))
            .sign(Algorithm.HMAC512(jwtSecret));
    return token;
  }
}
