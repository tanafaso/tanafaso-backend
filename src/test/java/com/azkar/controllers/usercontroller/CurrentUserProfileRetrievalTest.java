package com.azkar.controllers.usercontroller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.TestBase;
import com.azkar.entities.User;
import com.azkar.payload.usercontroller.GetUserResponse;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

public class CurrentUserProfileRetrievalTest extends TestBase {

  @Test
  public void getLoggedInUserProfile_shouldSucceed() throws Exception {
    String username = "example_username";
    String email = "example_email@example_domain.com";
    String name = "example_name";

    User user = User.builder()
        .username(username)
        .email(email)
        .name(name)
        .build();

    GetUserResponse expectedResponse = new GetUserResponse();
    expectedResponse.setData(user);

    addNewUser(user);
    ResultActions result =
        performGetRequest(user, "/me");

    result
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
  }
}
