package com.azkar;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.entities.User;
import com.azkar.factories.UserFactory;
import com.azkar.payload.homecontroller.GetHomeResponse;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;

public class HomeControllerTest extends BaseControllerTest {

  private static User user = UserFactory.getUser();

  @Before
  public void before() {
    authenticate(user);
  }

  @Test
  public void getHome_AuthenticatedUser_ShouldReturnUser() throws Exception {
    GetHomeResponse expectedResponse = new GetHomeResponse();
    expectedResponse.setData(user);

    prepareGetRequest("/")
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(mapToJson(expectedResponse)));
  }

}
