package com.azkar.controllers.authenticationcontroller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.TestBase;
import com.azkar.controllers.AuthenticationController;
import com.azkar.entities.User;
import com.azkar.factories.entities.UserFactory;
import com.azkar.factories.payload.requests.EmailLoginRequestBodyFactory;
import com.azkar.factories.payload.requests.EmailRegistrationRequestBodyFactory;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.authenticationcontroller.requests.EmailAuthenticationRequestBodyUtil;
import com.azkar.payload.authenticationcontroller.requests.EmailLoginRequestBody;
import com.azkar.payload.authenticationcontroller.requests.EmailRegistrationRequestBody;
import com.azkar.payload.authenticationcontroller.responses.EmailLoginResponse;
import com.azkar.payload.authenticationcontroller.responses.EmailRegistrationResponse;
import com.azkar.payload.exceptions.BadRequestException;
import com.azkar.payload.homecontroller.GetHomeResponse;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

public class LoginWithEmailTest extends TestBase {

  @Autowired
  PasswordEncoder passwordEncoder;

  @Test
  public void loginWithEmail_normalScenario_shouldReturnToken() throws Exception {
    String email = "example@domain.com";
    String password = "example_password";
    User user = UserFactory.getNewUserWithEmailAndEncodedPassword(email,
        passwordEncoder.encode(password));
    addNewUser(user);

    EmailLoginRequestBody emailLoginRequestBody =
        EmailLoginRequestBody.builder().email(email).password(password).build();
    MvcResult result = loginWithEmail(mapToJson(emailLoginRequestBody))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(new EmailLoginResponse())))
        .andReturn();

    // Validate the JWT returned by the API.
    GetHomeResponse expectedHomeResponse = new GetHomeResponse();
    expectedHomeResponse.setData(user);
    performGetRequest(result.getResponse().getHeader(HttpHeaders.AUTHORIZATION), "/")
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedHomeResponse)));
  }


  @Test
  public void loginWithEmail_emailMissingAtSign_shouldNotSucceed() throws Exception {
    final String emailWithoutAtSign = "test_emailtest.com";
    EmailLoginRequestBody body =
        EmailLoginRequestBodyFactory.getDefaultEmailLoginRequestBodyFactory();
    body.setEmail(emailWithoutAtSign);

    EmailLoginResponse expectedResponse = new EmailLoginResponse();
    expectedResponse.setError(new Error(EmailAuthenticationRequestBodyUtil.EMAIL_NOT_VALID_ERROR));
    loginWithEmail(mapToJson(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
  }

  @Test
  public void loginWithEmail_emailMissingDotSymbol_shouldNotSucceed() throws Exception {
    String emailWithoutDot = "test_email@testcom";
    EmailLoginRequestBody body =
        EmailLoginRequestBodyFactory.getDefaultEmailLoginRequestBodyFactory();
    body.setEmail(emailWithoutDot);

    EmailLoginResponse expectedResponse = new EmailLoginResponse();
    expectedResponse.setError(new Error(EmailAuthenticationRequestBodyUtil.EMAIL_NOT_VALID_ERROR));
    loginWithEmail(mapToJson(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
  }

  @Test
  public void loginWithEmail_passwordTooShort_shouldNotSucceed() throws Exception {
    String shortPassword = "abcdefg";
    EmailLoginRequestBody body =
        EmailLoginRequestBodyFactory.getDefaultEmailLoginRequestBodyFactory();
    body.setPassword(shortPassword);

    EmailLoginResponse expectedResponse = new EmailLoginResponse();
    expectedResponse
        .setError(
            new Error(EmailAuthenticationRequestBodyUtil.PASSWORD_CHARACTERS_LESS_THAN_MIN_ERROR));
    loginWithEmail(mapToJson(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
  }

  @Test
  public void loginWithEmail_emailNotProvided_shouldNotSucceed() throws Exception {
    EmailLoginRequestBody bodyMissingEmailField =
        EmailLoginRequestBodyFactory.getDefaultEmailLoginRequestBodyFactory();
    bodyMissingEmailField.setEmail(null);

    EmailLoginResponse expectedResponse = new EmailLoginResponse();
    expectedResponse
        .setError(new Error(BadRequestException.REQUIRED_FIELDS_NOT_GIVEN_ERROR));
    loginWithEmail(mapToJson(bodyMissingEmailField))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
  }

  @Test
  public void loginWithEmail_emailConfirmationSentAndStillPending_shouldNotSucceed()
      throws Exception {
    EmailRegistrationRequestBody emailRegistrationRequestBody =
        EmailRegistrationRequestBodyFactory.getDefaultEmailRegistrationRequestBody();

    performPutRequest(
        AuthenticationController.REGISTER_WITH_EMAIL_PATH, mapToJson(emailRegistrationRequestBody))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(new EmailRegistrationResponse())));

    EmailLoginRequestBody emailLoginRequestBody =
        EmailLoginRequestBody.builder().email(emailRegistrationRequestBody.getEmail())
            .password(emailRegistrationRequestBody.getPassword()).build();
    EmailLoginResponse expectedResponse = new EmailLoginResponse();
    expectedResponse.setError(new Error(EmailLoginResponse.EMAIL_NOT_VERIFIED_ERROR));

    loginWithEmail(mapToJson(emailLoginRequestBody))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
  }

  private ResultActions loginWithEmail(String body) throws Exception {
    return performPutRequest(
        AuthenticationController.LOGIN_WITH_EMAIL_PATH,
        body);
  }
}

