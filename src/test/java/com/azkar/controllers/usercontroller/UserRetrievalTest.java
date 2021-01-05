package com.azkar.controllers.usercontroller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.TestBase;
import com.azkar.controllers.utils.JsonHandler;
import com.azkar.entities.User;
import com.azkar.factories.entities.UserFactory;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.exceptions.DefaultExceptionResponse;
import com.azkar.payload.usercontroller.GetUserResponse;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

public class UserRetrievalTest extends TestBase {

  @Test
  public void getLoggedInUserProfile_shouldSucceed() throws Exception {
    User user = UserFactory.getNewUser();
    addNewUser(user);
    GetUserResponse expectedResponse = new GetUserResponse();
    expectedResponse.setData(user);

    ResultActions result = azkarApi.getProfile(user);

    result
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void getUserById_validUserId_shouldSucceed() throws Exception {
    String userId = "example id";
    User user = UserFactory.getNewUser().toBuilder().
        id(userId).
        build();
    addNewUser(user);
    GetUserResponse expectedResponse = new GetUserResponse();
    expectedResponse.setData(user);

    ResultActions result = performGetRequest(user, String.format("/users/%s", userId));

    result
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void getUserById_fakeUserId_shouldNotSucceed() throws Exception {
    String realUserId = "example id";
    String fakeUserId = "fake user id";
    User user = UserFactory.getNewUser().toBuilder().
        id(realUserId).
        build();
    addNewUser(user);
    GetUserResponse expectedResponse = new GetUserResponse();
    expectedResponse.setError(new Error(GetUserResponse.USER_NOT_FOUND_ERROR));

    ResultActions result = performGetRequest(user, String.format("/users/%s", fakeUserId));

    result
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void searchForUserByUsername_validUsername_shouldSucceed() throws Exception {
    String username = "example_username";
    User user = UserFactory.getNewUser().toBuilder().
        username(username).
        build();
    addNewUser(user);
    GetUserResponse expectedResponse = new GetUserResponse();
    expectedResponse.setData(user);

    ResultActions result = azkarApi.searchForUserByUsername(user, username);

    result
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void searchForUserByUsername_fakeUsername_shouldNotSucceed() throws Exception {
    String realUsername = "example-username";
    String fakeUsername = "fake-username";
    User user = UserFactory.getNewUser().toBuilder().
        username(realUsername).
        build();
    addNewUser(user);
    GetUserResponse expectedResponse = new GetUserResponse();
    expectedResponse.setError(new Error(GetUserResponse.USER_NOT_FOUND_ERROR));

    ResultActions result = azkarApi.searchForUserByUsername(user, fakeUsername);

    result
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void searchForUserByFacebookUserId_validFacebookUserId_shouldSucceed() throws Exception {
    String facebookUserId = "0123401234";
    User user = UserFactory.getUserRegisteredWithFacebookWithFacebookUserId(facebookUserId);
    addNewUser(user);
    GetUserResponse expectedResponse = new GetUserResponse();
    expectedResponse.setData(user);

    ResultActions result = azkarApi.searchForUserByFacebookUserId(user, facebookUserId);

    result
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void searchForUserByFacebookUserId_fakeFacebookUserId_shouldNotSucceed() throws Exception {
    String realFacebookUserId = "1234120";
    String fakeFacebookUserId = "23423413";
    User user = UserFactory.getUserRegisteredWithFacebookWithFacebookUserId(realFacebookUserId);
    addNewUser(user);
    GetUserResponse expectedResponse = new GetUserResponse();
    expectedResponse.setError(new Error(GetUserResponse.USER_NOT_FOUND_ERROR));

    ResultActions result = azkarApi.searchForUserByFacebookUserId(user, fakeFacebookUserId);

    result
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void searchForUserByUsername_wrongQueryParameterName_shouldNotSucceed() throws Exception {
    String username = "example-username";
    User user = UserFactory.getNewUser().toBuilder()
        .username(username)
        .build();
    addNewUser(user);
    DefaultExceptionResponse expectedResponse = new DefaultExceptionResponse();

    ResultActions result =
        performGetRequest(user, String.format("/users/search?wrong=%s", username));

    result
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void searchForUserByUsername_noQueryParametersSpecified_shouldNotSucceed()
      throws Exception {
    String username = "example-username";
    User user = UserFactory.getNewUser().toBuilder()
        .username(username)
        .build();
    addNewUser(user);

    GetUserResponse expectedResponse = new GetUserResponse();
    expectedResponse.setError(new Error(GetUserResponse.SEARCH_PARAMETERS_NOT_SPECIFIED));

    ResultActions result =
        performGetRequest(user, String.format("/users/search", username));

    result
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }
}
