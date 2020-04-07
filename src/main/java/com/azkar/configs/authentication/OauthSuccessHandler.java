package com.azkar.configs.authentication;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.azkar.entities.User;
import com.azkar.repos.UserRepo;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
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
      throws IOException {
    String email = ((DefaultOAuth2User) authentication.getPrincipal()).getAttribute("email");
    String name = ((DefaultOAuth2User) authentication.getPrincipal()).getAttribute("name");
    Optional<User> optionalUser = userRepo.findByEmail(email);
    User currentUser;
    if (optionalUser.isPresent()) {
      currentUser = optionalUser.get();
    } else {
      try {
        currentUser = buildNewUser(email, name);
        userRepo.save(currentUser);
      } catch (UsernameGenerationException e) {
        httpServletResponse
            .sendError(SC_INTERNAL_SERVER_ERROR, "Can not generate username for the new user.");
        return;
      }
    }
    String token = generateToken(currentUser);
    httpServletResponse.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
  }

  private String generateUsername(String email) throws UsernameGenerationException {
    String localPart = email.substring(0, email.indexOf('@'));
    final int kMaxLocalPartMatches = 100;
    final int kMaxGenerationTrials = 100;
    for (int i = 0; i < kMaxGenerationTrials; i++) {
      int randomSuffix = ThreadLocalRandom.current().nextInt(1, kMaxLocalPartMatches);
      String randomUsername = localPart + randomSuffix;
      if (!userRepo.findByUsername(randomUsername).isPresent()) {
        return randomUsername;
      }
    }
    throw new UsernameGenerationException();
  }

  private User buildNewUser(String email, String name) throws UsernameGenerationException {
    return User.builder().email(email).username(generateUsername(email)).name(name).build();
  }

  private String generateToken(User user) throws UnsupportedEncodingException {
    String token =
        JWT.create()
            .withSubject(user.getId())
            .withExpiresAt(new Date(System.currentTimeMillis() + TOKEN_TIMEOUT_IN_MILLIS))
            .sign(Algorithm.HMAC512(jwtSecret));
    return token;
  }
}
