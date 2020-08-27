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

  private final static String TEST_USER_FACEBOOK_TOKEN_ACCOUNT_1 =
      "EAADDR3MHJd8BALm8YlKLPHtVcnTOuSw0AqnbwZByGi5ouItrqBTWxdEDqsB3ZAMiewdm0O6v66hsF9RRS4MLsmk1OZCSL4MpTWA7sgG9shybi04GBwmeQnDtGzniqZB9dNZC57CUVopDHgcOxdSm7mItMkuRMttECIgdtm4GopExyZBfIJpD7H091M4K0RB3ofiXWZAe4ZBqXQZDZD";
  private final static String TEST_USER_FACEBOOK_ID_ACCOUNT_1 = "105160664522305";

  private final String TEST_USER_FACEBOOK_TOKEN_ACCOUNT_2 =
      "EAADDR3MHJd8BAN2ZARBB6G8qsrvhZCCSc7PwJRF0zNLQQDDZBQ21FWzkYuUhR9nZCSyaJTehL9480ybmCNMm2ghJlXNvrLoYvu6PlZCzFuTgFjdYXtLpyrFByQqOtyZCtXzDmCgTqZBJbmhi2VO3vQahNZBzucaEic0kS7gGOYic3LZA6j3NIvF0lloaEHTRxOYvz0fRZAx8OfNgZDZD";
  private final String TEST_USER_FACEBOOK_ID_ACCOUNT_2 = "109663874068160";

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

  private void loginWithFacebookSucceeded() throws Exception {
    FacebookAuthenticationRequest request = FacebookAuthenticationRequest.builder()
        .token(TEST_USER_FACEBOOK_TOKEN_ACCOUNT_1)
        .facebookUserId(TEST_USER_FACEBOOK_ID_ACCOUNT_1)
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
    assertThat(user.getUserFacebookData().getAccessToken(), is(TEST_USER_FACEBOOK_TOKEN_ACCOUNT_1));
    assertThat(user.getUserFacebookData().getUserId(), is(TEST_USER_FACEBOOK_ID_ACCOUNT_1));

    // Validate the JWT returned by the API.
    GetHomeResponse expectedHomeResponse = new GetHomeResponse();
    expectedHomeResponse.setData(user);
    performGetRequest(result.getResponse().getHeader(HttpHeaders.AUTHORIZATION), "/")
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedHomeResponse)));
  }

  @Test
  public void loginWithFacebook_userAlreadyLoggedIn_shouldNotSucceed() throws Exception {
    User loggedInUser = getLoggedInUser();
    FacebookAuthenticationRequest request = FacebookAuthenticationRequest.builder()
        .token(TEST_USER_FACEBOOK_TOKEN_ACCOUNT_1)
        .facebookUserId(TEST_USER_FACEBOOK_ID_ACCOUNT_1)
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
        .facebookUserId(TEST_USER_FACEBOOK_ID_ACCOUNT_1)
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
        .token(TEST_USER_FACEBOOK_TOKEN_ACCOUNT_1)
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

    assertConnectFacebookSucceeded(loggedInUser, TEST_USER_FACEBOOK_TOKEN_ACCOUNT_1,
        TEST_USER_FACEBOOK_ID_ACCOUNT_1);
  }

  @Test
  public void connectFacebook_forTheSecondTimeWithSameToken_shouldOverrideCredentials()
      throws Exception {
    User loggedInUser = getLoggedInUser();

    assertConnectFacebookSucceeded(loggedInUser, TEST_USER_FACEBOOK_TOKEN_ACCOUNT_1,
        TEST_USER_FACEBOOK_ID_ACCOUNT_1);

    assertConnectFacebookSucceeded(loggedInUser, TEST_USER_FACEBOOK_TOKEN_ACCOUNT_1,
        TEST_USER_FACEBOOK_ID_ACCOUNT_1);
  }

  @Test
  public void connectFacebook_forTheSecondTimeWithDifferentToken_shouldOverrideCredentials()
      throws Exception {
    User loggedInUser = getLoggedInUser();

    assertConnectFacebookSucceeded(loggedInUser, TEST_USER_FACEBOOK_TOKEN_ACCOUNT_1,
        TEST_USER_FACEBOOK_ID_ACCOUNT_1);

    assertConnectFacebookSucceeded(loggedInUser, TEST_USER_FACEBOOK_TOKEN_ACCOUNT_2,
        TEST_USER_FACEBOOK_ID_ACCOUNT_2);
  }

  @Test
  public void connectFacebook_anotherUserConnectedWithSameAccount_shouldNotSucceed()
      throws Exception {
    User loggedInUser1 = getLoggedInUser();

    assertConnectFacebookSucceeded(loggedInUser1, TEST_USER_FACEBOOK_TOKEN_ACCOUNT_1,
        TEST_USER_FACEBOOK_ID_ACCOUNT_1);

    User loggedInUser2 = getLoggedInUser();
    FacebookAuthenticationRequest request = FacebookAuthenticationRequest.builder()
        .token(TEST_USER_FACEBOOK_TOKEN_ACCOUNT_1)
        .facebookUserId(TEST_USER_FACEBOOK_ID_ACCOUNT_1)
        .build();
    FacebookAuthenticationResponse expectedAuthenticationResponse =
        new FacebookAuthenticationResponse();
    expectedAuthenticationResponse
        .setError(new Error(FacebookAuthenticationResponse.SOMEONE_ELSE_ALREADY_CONNECTED_ERROR));

    performPutRequest(loggedInUser2, AuthenticationController.CONNECT_WITH_FACEBOOK_PATH,
        mapToJson(request))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedAuthenticationResponse)))
        .andReturn();

    User loggedInUser1InRepo = userRepo.findById(loggedInUser1.getId()).get();
    assertThat(loggedInUser1InRepo.getUserFacebookData().getAccessToken(),
        is(TEST_USER_FACEBOOK_TOKEN_ACCOUNT_1));
    assertThat(loggedInUser1InRepo.getUserFacebookData().getUserId(),
        is(TEST_USER_FACEBOOK_ID_ACCOUNT_1));

    User loggedInUser2InRepo = userRepo.findById(loggedInUser2.getId()).get();
    assertThat(loggedInUser2InRepo.getUserFacebookData(), is(nullValue()));
  }

  @Test
  public void connectFacebook_wrongFacebookToken_shouldNotSucceed() throws Exception {
    User loggedInUser = getLoggedInUser();

    final String wrongFacebookToken = "wrongFacebookToken";
    FacebookAuthenticationRequest request = FacebookAuthenticationRequest.builder()
        .token(wrongFacebookToken)
        .facebookUserId(TEST_USER_FACEBOOK_ID_ACCOUNT_1)
        .build();
    FacebookAuthenticationResponse expectedAuthenticationResponse =
        new FacebookAuthenticationResponse();
    expectedAuthenticationResponse
        .setError(new Error(FacebookAuthenticationResponse.AUTHENTICATION_WITH_FACEBOOK_ERROR));

    performPutRequest(loggedInUser, AuthenticationController.CONNECT_WITH_FACEBOOK_PATH,
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
        .token(TEST_USER_FACEBOOK_TOKEN_ACCOUNT_1)
        .facebookUserId(wrongFacebookUserId)
        .build();
    FacebookAuthenticationResponse expectedAuthenticationResponse =
        new FacebookAuthenticationResponse();
    expectedAuthenticationResponse
        .setError(new Error(FacebookAuthenticationResponse.AUTHENTICATION_WITH_FACEBOOK_ERROR));

    performPutRequest(loggedInUser, AuthenticationController.CONNECT_WITH_FACEBOOK_PATH,
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
        .token(TEST_USER_FACEBOOK_TOKEN_ACCOUNT_1)
        .facebookUserId(TEST_USER_FACEBOOK_ID_ACCOUNT_1)
        .build();

    // TODO(omar): Add expected response here after standardizing responses for unauthenticated
    //  users.
    performPutRequest(AuthenticationController.CONNECT_WITH_FACEBOOK_PATH,
        mapToJson(request))
        .andExpect(status().is3xxRedirection())
        .andReturn();

    assertThat(userRepo.count(), is(0L));
  }

  private void assertConnectFacebookSucceeded(User loggedInUser, String testUserFacebookToken
      , String testUserFacebookId) throws Exception {
    FacebookAuthenticationRequest request = FacebookAuthenticationRequest.builder()
        .token(testUserFacebookToken)
        .facebookUserId(testUserFacebookId)
        .build();
    FacebookAuthenticationResponse expectedAuthenticationResponse =
        new FacebookAuthenticationResponse();

    performPutRequest(loggedInUser, AuthenticationController.CONNECT_WITH_FACEBOOK_PATH,
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
