package com.azkar.controllers.authenticationcontroller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.TestBase;
import com.azkar.controllers.AuthenticationController;
import com.azkar.entities.User;
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

public class FacebookAuthenticationTest extends TestBase {

  private final static String testUserFacebookTokenAccount1 =
      "EAADDR3MHJd8BAHmY7XY8zlGftGdNlx9d473i1dPTH0ycYjC0weIibBtSegZAkX8p0eqZCfSgYSlRgZA58MugRmewwnruiqugqoIn5KBCU8DIz4DM1MMn38mTQrNXZCWWQrtqonjfgZBmhxjkcgi7OBgKUt5HlUtLOSQBSUUE5gZBTTOBNYKZAJrHcKOsnFDgw5u0WFzSZC6EMAZDZD";
  private final static String testUserFacebookIdAccount1 = "105160664522305";

  private final String testUserFacebookTokenAccount2 =
      "EAADDR3MHJd8BAIXVDbiqFl6WYoPvS5JMoVg3E0CbTTviPlLM9N0wxT7s1ZA7BHL5p9ZBA9JEpFJqtjMGNZCoxZBymvJnYXIDnZAdVGImXTREL8ikexaPpY3ixrrOfvq0GrGOMNjJg1SUYKzhBy8liJ9j9xjeWLZBIIFMqJFSn4poVgjbvnlrUJB8NtKohaWWQie9iOu4LZBigZDZD";
  private final String testUserFacebookIdAccount2 = "109663874068160";

  @Autowired
  UserRepo userRepo;

  @Test
  public void loginWithFacebook_forTheFirstTime_shouldSucceed() throws Exception {
    loginWithFacebookSucceeded();
  }

  @Test
  public void loginWithFacebook_forTheSecondTime_shouldSucceed() throws Exception {
    loginWithFacebookSucceeded();
    loginWithFacebookSucceeded();
  }

  @Test
  public void loginWithFacebook_userAlreadyLoggedIn_shouldNotSucceed() throws Exception {
    User loggedInUser = getLoggedInUser();
    FacebookAuthenticationRequest request = FacebookAuthenticationRequest.builder()
        .token(testUserFacebookTokenAccount1)
        .facebookUserId(testUserFacebookIdAccount1)
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
        .facebookUserId(testUserFacebookIdAccount1)
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
        .token(testUserFacebookTokenAccount1)
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

  @Test
  public void connectFacebook_forTheFirstTime_shouldSucceed() throws Exception {
    User loggedInUser = getLoggedInUser();

    assertConnectFacebookSucceeded(loggedInUser, testUserFacebookTokenAccount1,
        testUserFacebookIdAccount1);
  }

  @Test
  public void connectFacebook_forTheSecondTimeWithSameToken_shouldOverrideCredentials()
      throws Exception {
    User loggedInUser = getLoggedInUser();

    assertConnectFacebookSucceeded(loggedInUser, testUserFacebookTokenAccount1,
        testUserFacebookIdAccount1);

    assertConnectFacebookSucceeded(loggedInUser, testUserFacebookTokenAccount1,
        testUserFacebookIdAccount1);
  }

  @Test
  public void connectFacebook_forTheSecondTimeWithDifferentToken_shouldOverrideCredentials()
      throws Exception {
    User loggedInUser = getLoggedInUser();

    assertConnectFacebookSucceeded(loggedInUser, testUserFacebookTokenAccount1,
        testUserFacebookIdAccount1);

    assertConnectFacebookSucceeded(loggedInUser, testUserFacebookTokenAccount2,
        testUserFacebookIdAccount2);
  }

  @Test
  public void connectFacebook_anotherUserConnectedWithSameAccount_shouldNotSucceed()
      throws Exception {
    User loggedInUser1 = getLoggedInUser();

    assertConnectFacebookSucceeded(loggedInUser1, testUserFacebookTokenAccount1,
        testUserFacebookIdAccount1);

    User loggedInUser2 = getLoggedInUser();
    FacebookAuthenticationRequest request = FacebookAuthenticationRequest.builder()
        .token(testUserFacebookTokenAccount1)
        .facebookUserId(testUserFacebookIdAccount1)
        .build();
    FacebookAuthenticationResponse expectedAuthenticationResponse =
        new FacebookAuthenticationResponse();
    expectedAuthenticationResponse
        .setError(new Error(FacebookAuthenticationResponse.SOMEONE_ELSE_ALREADY_CONNECTED_ERROR));

    performPutRequest(loggedInUser2, "/connect/facebook",
        mapToJson(request))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedAuthenticationResponse)))
        .andReturn();

    User loggedInUser1InRepo = userRepo.findById(loggedInUser1.getId()).get();
    assertThat(loggedInUser1InRepo.getUserFacebookData().getAccessToken(),
        is(testUserFacebookTokenAccount1));
    assertThat(loggedInUser1InRepo.getUserFacebookData().getUserId(),
        is(testUserFacebookIdAccount1));

    User loggedInUser2InRepo = userRepo.findById(loggedInUser2.getId()).get();
    assertThat(loggedInUser2InRepo.getUserFacebookData(), is(nullValue()));
  }

  @Test
  public void connectFacebook_wrongFacebookToken_shouldNotSucceed() throws Exception {
    User loggedInUser = getLoggedInUser();

    final String wrongFacebookToken = "wrongFacebookToken";
    FacebookAuthenticationRequest request = FacebookAuthenticationRequest.builder()
        .token(wrongFacebookToken)
        .facebookUserId(testUserFacebookIdAccount1)
        .build();
    FacebookAuthenticationResponse expectedAuthenticationResponse =
        new FacebookAuthenticationResponse();
    expectedAuthenticationResponse
        .setError(new Error(FacebookAuthenticationResponse.AUTHENTICATION_WITH_FACEBOOK_ERROR));

    performPutRequest(loggedInUser, "/connect/facebook",
        mapToJson(request))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedAuthenticationResponse)))
        .andReturn();

    User loggedInUserInRepo = Iterators.getOnlyElement(userRepo.findAll().iterator());
    assertThat(loggedInUserInRepo.getUserFacebookData(), is(nullValue()));
  }

  @Test
  public void connectFacebook_wrongUserId_shouldNotSucceed() throws Exception {
    User loggedInUser = getLoggedInUser();

    final String wrongFacebookUserId = "wrongUserId";
    FacebookAuthenticationRequest request = FacebookAuthenticationRequest.builder()
        .token(testUserFacebookTokenAccount1)
        .facebookUserId(wrongFacebookUserId)
        .build();
    FacebookAuthenticationResponse expectedAuthenticationResponse =
        new FacebookAuthenticationResponse();
    expectedAuthenticationResponse
        .setError(new Error(FacebookAuthenticationResponse.AUTHENTICATION_WITH_FACEBOOK_ERROR));

    performPutRequest(loggedInUser, "/connect/facebook",
        mapToJson(request))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedAuthenticationResponse)))
        .andReturn();

    User loggedInUserInRepo = Iterators.getOnlyElement(userRepo.findAll().iterator());
    assertThat(loggedInUserInRepo.getUserFacebookData(), is(nullValue()));
  }


  @Test
  public void connectFacebook_noLoggedInUser_shouldBeRedirected() throws Exception {
    FacebookAuthenticationRequest request = FacebookAuthenticationRequest.builder()
        .token(testUserFacebookTokenAccount1)
        .facebookUserId(testUserFacebookIdAccount1)
        .build();

    // TODO(omar): Add expected response here after standardizing responses for unauthenticated
    //  users.
    performPutRequest("/connect/facebook",
        mapToJson(request))
        .andExpect(status().is3xxRedirection())
        .andReturn();

    assertThat(userRepo.count(), is(0L));
  }

/*
  @Test
  public void connectFacebook_noLoggedInUser_shouldBeRedirected() throws Exception {
    FacebookAuthenticationRequest request = FacebookAuthenticationRequest.builder()
        .token(testUserFacebookTokenAccount1)
        .facebookUserId(testUserFacebookIdAccount1)
        .build();

    // TODO(omar): Add expected response here after standardizing responses for unauthenticated
    //  users.
    performPutRequest("/connect/facebook",
        mapToJson(request))
        .andExpect(status().is3xxRedirection())
        .andReturn();

    assertThat(userRepo.count(), is(0L));
  }
*/

  private void loginWithFacebookSucceeded() throws Exception {
    FacebookAuthenticationRequest request = FacebookAuthenticationRequest.builder()
        .token(testUserFacebookTokenAccount1)
        .facebookUserId(testUserFacebookIdAccount1)
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
    assertThat(user.getUserFacebookData().getAccessToken(), is(testUserFacebookTokenAccount1));
    assertThat(user.getUserFacebookData().getUserId(), is(testUserFacebookIdAccount1));

    // Validate the JWT returned by the API.
    setForcedJwtToken(result.getResponse().getHeader(HttpHeaders.AUTHORIZATION));
    GetHomeResponse expectedHomeResponse = new GetHomeResponse();
    expectedHomeResponse.setData(user);
    performGetRequest(user, "/")
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedHomeResponse)));
  }

  private void assertConnectFacebookSucceeded(User loggedInUser, String testUserFacebookToken
      , String testUserFacebookId) throws Exception {
    FacebookAuthenticationRequest request = FacebookAuthenticationRequest.builder()
        .token(testUserFacebookToken)
        .facebookUserId(testUserFacebookId)
        .build();
    FacebookAuthenticationResponse expectedAuthenticationResponse =
        new FacebookAuthenticationResponse();

    performPutRequest(loggedInUser, "/connect/facebook",
        mapToJson(request))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedAuthenticationResponse)))
        .andReturn();

    User user =
        Iterators.getOnlyElement(userRepo.findAll().iterator());
    assertThat(user.getUserFacebookData().getAccessToken(), is(testUserFacebookToken));
    assertThat(user.getUserFacebookData().getUserId(), is(testUserFacebookId));
  }
}
