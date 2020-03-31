package com.omar.azkar.controllers;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class AuthenticationController {

  @Autowired
  private OAuth2AuthorizedClientService authorizedClientService;

  @GetMapping("/loginSuccess")
  public AuthenticationControllerResponse getLoginInfo(OAuth2AuthenticationToken authentication) {
    OAuth2AuthorizedClient client = authorizedClientService
        .loadAuthorizedClient(
            authentication.getAuthorizedClientRegistrationId(),
            authentication.getName());

    String userInfoEndpointUri = client.getClientRegistration()
        .getProviderDetails().getUserInfoEndpoint().getUri();

    if (!StringUtils.isEmpty(userInfoEndpointUri)) {
      RestTemplate restTemplate = new RestTemplate();
      HttpHeaders headers = new HttpHeaders();
      headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + client.getAccessToken()
          .getTokenValue());
      HttpEntity httpEntity = new HttpEntity("", headers);
      ResponseEntity<Map> response = restTemplate
          .exchange(userInfoEndpointUri, HttpMethod.GET, httpEntity, Map.class);
      Map userAttributes = response.getBody();
      return new AuthenticationControllerResponse(true, (String) userAttributes.get("email"));
    }
    return new AuthenticationControllerResponse(false, null);
  }

  private static class AuthenticationControllerResponse extends Response {

    private String userEmail;

    public AuthenticationControllerResponse(boolean success, String userEmail) {
      super(success);
      this.userEmail = userEmail;
    }

    public void setUserEmail(String userEmail) {
      this.userEmail = userEmail;
    }

    public String getUserEmail() {
      return userEmail;
    }
  }
}
