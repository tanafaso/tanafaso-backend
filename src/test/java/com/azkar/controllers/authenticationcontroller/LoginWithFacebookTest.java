package com.azkar.controllers.authenticationcontroller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.TestBase;
import com.azkar.controllers.AuthenticationController;
import com.azkar.entities.User;
import com.azkar.factories.entities.UserFactory;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.authenticationcontroller.requests.FacebookAuthenticationRequest;
import com.azkar.payload.authenticationcontroller.responses.FacebookAuthenticationResponse;
import com.azkar.payload.homecontroller.GetHomeResponse;
import com.azkar.repos.UserRepo;
import com.google.common.collect.Iterators;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class LoginWithFacebookTest extends TestBase {

  private final static String testUserFacebookToken =
      "EAADDR3MHJd8BAGpt4XZCYsLZCbYsTgUmkMog5hnmJol5LlEZC1fuRRKaAvcCtbRhTZCrewHlvwtyTZA0efgtBTzVYJahI5aHBZATjWCQO1ZB9ryl4c8WQtSCm7YGGzFjSrYK1JApDR6S5UrRuuVilmobqGNTXbDzFlPHRZCgTGpOjGqw0LMyUPs3HC1f3JGEewWAKhSlxrFnZBQZDZD";
  private final static String testUserFacebookId = "105160664522305";

  @Autowired
  UserRepo userRepo;

  @Test
  public void loginWithFacebook_forTheFirstTime_shouldSucceed() throws Exception {
    FacebookAuthenticationRequest request = FacebookAuthenticationRequest.builder()
        .token(testUserFacebookToken)
        .facebookUserId(testUserFacebookId)
        .build();
    FacebookAuthenticationResponse expectedAuthenticationResponse =
        new FacebookAuthenticationResponse();

    MvcResult result = performPutRequest(AuthenticationController.LOGIN_WITH_FACEBOOK_PATH,
        mapToJson(request))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedAuthenticationResponse)))
        .andExpect(header().exists(HttpHeaders.AUTHORIZATION))
        .andReturn();

    User user =
        Iterators.getOnlyElement(userRepo.findAll().iterator());
    assertThat(user.getUserFacebookData().getAccessToken(), is(testUserFacebookToken));
    assertThat(user.getUserFacebookData().getUserId(), is(testUserFacebookId));

    // Validate the JWT returned by the API.
    setForcedJwtToken(result.getResponse().getHeader(HttpHeaders.AUTHORIZATION));
    GetHomeResponse expectedHomeResponse = new GetHomeResponse();
    expectedHomeResponse.setData(user);
    performGetRequest(user, "/")
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedHomeResponse)));
  }

  @Test
  public void loginWithFacebook_forTheSecondTime_shouldSucceed() throws Exception {
    FacebookAuthenticationRequest request = FacebookAuthenticationRequest.builder()
        .token(testUserFacebookToken)
        .facebookUserId(testUserFacebookId)
        .build();
    FacebookAuthenticationResponse expectedAuthenticationResponse =
        new FacebookAuthenticationResponse();

    assertThat(userRepo.count(), is(0L));
    performPutRequest(AuthenticationController.LOGIN_WITH_FACEBOOK_PATH,
        mapToJson(request))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedAuthenticationResponse)))
        .andExpect(header().exists(HttpHeaders.AUTHORIZATION))
        .andReturn();
    assertThat(userRepo.count(), is(1L));
    MvcResult result = performPutRequest(AuthenticationController.LOGIN_WITH_FACEBOOK_PATH,
        mapToJson(request))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedAuthenticationResponse)))
        .andExpect(header().exists(HttpHeaders.AUTHORIZATION))
        .andReturn();

    User user =
        Iterators.getOnlyElement(userRepo.findAll().iterator());
    assertThat(user.getUserFacebookData().getAccessToken(), is(testUserFacebookToken));
    assertThat(user.getUserFacebookData().getUserId(), is(testUserFacebookId));

    // Validate the JWT returned by the API.
    setForcedJwtToken(result.getResponse().getHeader(HttpHeaders.AUTHORIZATION));
    GetHomeResponse expectedHomeResponse = new GetHomeResponse();
    expectedHomeResponse.setData(user);
    performGetRequest(user, "/")
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedHomeResponse)));
  }

  @Test
  public void loginWithFacebook_userAlreadyLoggedIn_shouldNotSucceed() throws Exception {
    User loggedInUser = UserFactory.getNewUser();
    addNewUser(loggedInUser);
    FacebookAuthenticationRequest request = FacebookAuthenticationRequest.builder()
        .token(testUserFacebookToken)
        .facebookUserId(testUserFacebookId)
        .build();
    FacebookAuthenticationResponse expectedAuthenticationResponse =
        new FacebookAuthenticationResponse();
    expectedAuthenticationResponse
        .setError(new Error(FacebookAuthenticationResponse.USER_ALREADY_LOGGED_IN));
    assertThat(userRepo.count(), is(1L));

    performPutRequest(loggedInUser,
        AuthenticationController.LOGIN_WITH_FACEBOOK_PATH,
        mapToJson(request))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedAuthenticationResponse)))
        .andExpect(header().doesNotExist(HttpHeaders.AUTHORIZATION))
        .andReturn();

    assertThat(userRepo.count(), is(1L));
  }

  @Test
  public void loginWithFacebook_wrongFacebookToken_shouldNotSucceed() throws Exception {
    final String wrongFacebookToken = "wrongFacebookToken";
    FacebookAuthenticationRequest request = FacebookAuthenticationRequest.builder()
        .token(wrongFacebookToken)
        .facebookUserId(testUserFacebookId)
        .build();
    FacebookAuthenticationResponse expectedAuthenticationResponse =
        new FacebookAuthenticationResponse();
    expectedAuthenticationResponse
        .setError(new Error(FacebookAuthenticationResponse.AUTHENTICATION_WITH_FACEBOOK_ERROR));

    performPutRequest(AuthenticationController.LOGIN_WITH_FACEBOOK_PATH,
        mapToJson(request))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedAuthenticationResponse)))
        .andExpect(header().doesNotExist(HttpHeaders.AUTHORIZATION))
        .andReturn();

    assertThat(userRepo.count(), is(0L));
  }

  @Test
  public void loginWithFacebook_wrongFacebookUserId_shouldNotSucceed() throws Exception {
    final String wrongFacebookUserId = "wrongUserId";
    FacebookAuthenticationRequest request = FacebookAuthenticationRequest.builder()
        .token(testUserFacebookToken)
        .facebookUserId(wrongFacebookUserId)
        .build();
    FacebookAuthenticationResponse expectedAuthenticationResponse =
        new FacebookAuthenticationResponse();
    expectedAuthenticationResponse
        .setError(new Error(FacebookAuthenticationResponse.AUTHENTICATION_WITH_FACEBOOK_ERROR));

    performPutRequest(AuthenticationController.LOGIN_WITH_FACEBOOK_PATH,
        mapToJson(request))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedAuthenticationResponse)))
        .andExpect(header().doesNotExist(HttpHeaders.AUTHORIZATION))
        .andReturn();

    assertThat(userRepo.count(), is(0L));
  }
}
