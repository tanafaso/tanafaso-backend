package com.omar.azkar.configs.Authentication;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.gson.Gson;
import com.omar.azkar.entities.User;
import com.omar.azkar.repos.UserRepo;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Optional;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OauthSuccessHandler implements AuthenticationSuccessHandler {
  private static int TOKEN_TIMEOUT_IN_MILLIS = 7*24*60*60*1000; // 7 days

  @Autowired
  UserRepo userRepo;

  @Value("${app.jwtSecret}")
  String jwtSecret;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse, Authentication authentication)
      throws IOException, ServletException {
    String email = ((DefaultOidcUser)authentication.getPrincipal()).getAttribute("email");
    String name = ((DefaultOidcUser)authentication.getPrincipal()).getAttribute("name");
    Optional<User> optionalUser = userRepo.findByEmail(email);
    User currentUser;
    if(optionalUser.isPresent()) {
      currentUser = optionalUser.get();
    } else {
      User newUser = new User();
      newUser.setName(name);
      newUser.setEmail(email);
      currentUser = userRepo.save(newUser);
    }
    String token = generateToken(currentUser.getId());
    httpServletResponse.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
  }

  public String generateToken(String id) throws UnsupportedEncodingException {
    String token = JWT.create().withSubject(id).withExpiresAt(new Date(System.currentTimeMillis()+ TOKEN_TIMEOUT_IN_MILLIS))
        .sign(Algorithm.HMAC512(jwtSecret));
    return token;
  }
}
