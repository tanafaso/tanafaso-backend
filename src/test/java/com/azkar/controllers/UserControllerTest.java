package com.azkar.controllers;

import com.azkar.factories.UserPrincipalFactory;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@WebMvcTest(UserController.class)
public class UserControllerTest {

  @Mock
  private BaseController baseController;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void getFriends() {
    Mockito.when(baseController.getCurrentUser())
        .thenReturn(UserPrincipalFactory.getUserPrincipal());

    mockMvc
        .perform(MockMvcRequestBuilders.get("/users/friends").accept(MediaType.APPLICATION_JSON));
  }

}
