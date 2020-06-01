package com.azkar;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.entities.User;
import com.azkar.factories.entities.UserFactory;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.exceptions.DefaultExceptionResponse;
import org.junit.Test;
import org.springframework.http.MediaType;

public class DefaultExceptionHandlingTest extends TestBase {

  @Test
  public void invalidRequest_handlerNotSupportingHttpMethod_shouldNotSucceed() throws Exception {
    User user = UserFactory.getNewUser();
    addNewUser(user);

    DefaultExceptionResponse expectedResponse = new DefaultExceptionResponse();
    expectedResponse.setError(new Error(DefaultExceptionResponse.DEFAULT_ERROR));
    performPutRequest(user, "/users", /*body=*/null)
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
  }
}
