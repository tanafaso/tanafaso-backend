package com.azkar.controllers.authenticationcontroller;

import com.azkar.TestBase;
import com.azkar.entities.User;
import com.azkar.factories.entities.UserFactory;
import com.azkar.payload.ResponseBase.Status;
import com.azkar.payload.authenticationcontroller.responses.UnauthenticatedResponse;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ExpiredJwtTokenTest extends TestBase {

  @Test
  public void getLoggedInUserProfile_expiredTokenProvided_shouldSucceed() throws Exception {
    User user = UserFactory.getNewUser();
    addNewUser(user);
    ResultActions result =
        performGetRequest(httpClient.getExpiredAuthenticationToken(user), "/users/me");

    result
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }
}
