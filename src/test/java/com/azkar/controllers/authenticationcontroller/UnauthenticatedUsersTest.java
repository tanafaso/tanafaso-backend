package com.azkar.controllers.authenticationcontroller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.TestBase;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.authenticationcontroller.responses.UnauthenticatedResponse;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public class UnauthenticatedUsersTest extends TestBase {

  @Test
  public void getLoggedInUserProfile_invalidTokenProvided_shouldNotSucceed() throws Exception {
    UnauthenticatedResponse expectedResponse = new UnauthenticatedResponse();
    expectedResponse.setError(new Error(UnauthenticatedResponse.AUTHENTICATION_ERROR));

    ResultActions result =
        performGetRequest(/*token=*/"invalid-token-example", "/users/me");

    result
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
  }

  @Test
  public void getLoggedInUserProfile_noTokenProvided_shouldNotSucceed() throws Exception {
    UnauthenticatedResponse expectedResponse = new UnauthenticatedResponse();
    expectedResponse.setError(new Error(UnauthenticatedResponse.AUTHENTICATION_ERROR));

    MockHttpServletRequestBuilder requestBuilder = get("/users/me");
    ResultActions result = mockMvc.perform(requestBuilder);

    result
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
  }
}
