package com.omar.azkar.controllers;

import java.io.UnsupportedEncodingException;
import java.util.Date;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthenticationController {

  @Autowired
  private OAuth2AuthorizedClientService authorizedClientService;

  @Value("${app.jwtSecret}")
  private String jwtSecret;

  @GetMapping("/loginSuccess")
  public ResponseEntity<AuthenticationControllerResponse> getLoginInfo(
      OAuth2AuthenticationToken authentication) throws UnsupportedEncodingException {
    // TODO: Get id from database using info in "authentication"
    String token = JWT.create().withSubject("5e837b4babae1b83f1b636b0")
        .withExpiresAt(new Date(System.currentTimeMillis() + 86400000 /* 1 day */))
        .sign(Algorithm.HMAC512(jwtSecret));

    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add("Authorization", "Bearer " + token);
    return new ResponseEntity<>(new AuthenticationControllerResponse(true, token),
        httpHeaders,
        HttpStatus.OK);
  }

  private static class AuthenticationControllerResponse extends Response {

    private String token;

    public AuthenticationControllerResponse(boolean success, String token) {
      super(success);
      this.token = token;
    }

    public void setToken(String userEmail) {
      this.token = token;
    }

    public String getToken() {
      return token;
    }
  }
}
