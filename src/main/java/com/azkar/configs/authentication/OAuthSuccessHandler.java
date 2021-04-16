package com.azkar.configs.authentication;

import com.azkar.entities.User;
import com.azkar.repos.UserRepo;
import com.azkar.services.JwtService;
import com.azkar.services.UserService;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

  private static final Logger logger = LoggerFactory.getLogger(OAuthSuccessHandler.class);

  @Autowired
  UserRepo userRepo;

  @Autowired
  UserService userService;

  @Autowired
  JwtService jwtService;


  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse,
      Authentication authentication)
      throws IOException {
    String email = ((DefaultOAuth2User) authentication.getPrincipal()).getAttribute("email");
    String firstName =
        ((DefaultOAuth2User) authentication.getPrincipal()).getAttribute("first_name");
    String lastName = ((DefaultOAuth2User) authentication.getPrincipal()).getAttribute("last_name");

    Optional<User> optionalUser = userRepo.findByEmail(email);
    User currentUser;
    if (optionalUser.isPresent()) {
      logger.info(String.format("Successful Oauth for user: %s with email: %s",
          firstName + " " + lastName,
          email));

      currentUser = optionalUser.get();
    } else {
      logger.info(String.format("First successful Oauth for user: %s with email: %s",
          firstName + " " + lastName,
          email));

      currentUser = userService.buildNewUser(email, firstName, lastName);
      userService.addNewUser(currentUser);
    }
    String token = jwtService.generateToken(currentUser);
    httpServletResponse.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
  }

}
