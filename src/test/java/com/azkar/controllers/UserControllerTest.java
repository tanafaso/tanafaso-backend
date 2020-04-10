package com.azkar.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.factories.UserPrincipalFactory;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
public class UserControllerTest extends ControllerTest {

  @Test
  void getFriends() throws Exception {
    Mockito.when(baseController.getCurrentUser())
        .thenReturn(UserPrincipalFactory.getUserPrincipal());

    mockMvc.perform(get("/users/friends")
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

}
