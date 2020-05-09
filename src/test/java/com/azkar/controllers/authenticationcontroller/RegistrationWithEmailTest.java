package com.azkar.controllers.authenticationcontroller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.TestBase;
import com.azkar.controllers.AuthenticationController;
import com.azkar.entities.RegistrationEmailConfirmationState;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.authenticationcontroller.requests.EmailRegistrationRequestBody;
import com.azkar.payload.authenticationcontroller.responses.EmailRegistrationResponse;
import com.azkar.payload.exceptions.BadRequestException;
import com.azkar.repos.RegistrationEmailConfirmationStateRepo;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

public class RegistrationWithEmailTest extends TestBase {

  @Autowired
  RegistrationEmailConfirmationStateRepo registrationEmailConfirmationStateRepo;

  @Autowired
  PasswordEncoder passwordEncoder;

  @Test
  public void registerWithEmail_normalScenario_shouldAddStateEntryForBodyParams() throws Exception {
    EmailRegistrationRequestBody body = EmailRegistrationRequestBody.builder()
        .email("test_email@test.com")
        .name("test_name")
        .password("test_password")
        .build();

    EmailRegistrationResponse expectedResponse = new EmailRegistrationResponse();

    assertThat(registrationEmailConfirmationStateRepo.count(), is(0L));
    registerWithEmail(mapToJson(body))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));

    assertThat(registrationEmailConfirmationStateRepo.count(), is(1L));
    RegistrationEmailConfirmationState state =
        registrationEmailConfirmationStateRepo.findAll().get(0);
    assertThat(state.getName(), is(body.getName()));
    assertThat(state.getEmail(), is(body.getEmail()));
    assertThat("Password is hashed and saved",
        passwordEncoder.matches(body.getPassword(), state.getPassword()));
  }

  @Test
  public void registerWithEmail_normalScenario_shouldAddStateEntryWithSixDigitsPin()
      throws Exception {
    EmailRegistrationRequestBody body = EmailRegistrationRequestBody.builder()
        .email("test_email@test.com")
        .name("test_name")
        .password("test_password")
        .build();

    EmailRegistrationResponse expectedResponse = new EmailRegistrationResponse();

    assertThat(registrationEmailConfirmationStateRepo.count(), is(0L));
    registerWithEmail(mapToJson(body))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));

    assertThat(registrationEmailConfirmationStateRepo.count(), is(1L));
    RegistrationEmailConfirmationState state =
        registrationEmailConfirmationStateRepo.findAll().get(0);
    assertThat((state.getPin() + "").length(), is(6));
  }

  @Test
  public void registerWithEmail_emailNotValid1_shouldNotSucceed() throws Exception {
    EmailRegistrationRequestBody body = EmailRegistrationRequestBody.builder()
        .email("test_emailtest.com") // missing '@'
        .name("test_name")
        .password("test_password")
        .build();

    EmailRegistrationResponse expectedResponse = new EmailRegistrationResponse();
    expectedResponse.setError(new Error(EmailRegistrationRequestBody.EMAIL_NOT_VALID_ERROR));
    assertThat(registrationEmailConfirmationStateRepo.count(), is(0L));
    registerWithEmail(mapToJson(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
    assertThat(registrationEmailConfirmationStateRepo.count(), is(0L));
  }

  @Test
  public void registerWithEmail_emailNotValid2_shouldNotSucceed() throws Exception {
    EmailRegistrationRequestBody body = EmailRegistrationRequestBody.builder()
        .email("test_email@testcom") // missing '.'
        .name("test_name")
        .password("test_password")
        .build();

    EmailRegistrationResponse expectedResponse = new EmailRegistrationResponse();
    expectedResponse.setError(new Error(EmailRegistrationRequestBody.EMAIL_NOT_VALID_ERROR));
    assertThat(registrationEmailConfirmationStateRepo.count(), is(0L));
    registerWithEmail(mapToJson(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
    assertThat(registrationEmailConfirmationStateRepo.count(), is(0L));
  }

  @Test
  public void registerWithEmail_passwordNotValid_shouldNotSucceed() throws Exception {
    EmailRegistrationRequestBody body = EmailRegistrationRequestBody.builder()
        .email("test_email@test.com")
        .name("test_name")
        .password("abcdefg") // less than 8 characters
        .build();

    EmailRegistrationResponse expectedResponse = new EmailRegistrationResponse();
    expectedResponse
        .setError(new Error(EmailRegistrationRequestBody.PASSWORD_CHARACTERS_LESS_THAN_MIN_ERROR));
    assertThat(registrationEmailConfirmationStateRepo.count(), is(0L));
    registerWithEmail(mapToJson(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
    assertThat(registrationEmailConfirmationStateRepo.count(), is(0L));
  }

  @Test
  public void registerWithEmail_nameEmpty_shouldNotSucceed() throws Exception {
    EmailRegistrationRequestBody body = EmailRegistrationRequestBody.builder()
        .email("test_email@test.com")
        .name("")
        .password("abcdefge")
        .build();

    EmailRegistrationResponse expectedResponse = new EmailRegistrationResponse();
    expectedResponse
        .setError(new Error(EmailRegistrationRequestBody.NAME_EMPTY_ERROR));
    assertThat(registrationEmailConfirmationStateRepo.count(), is(0L));
    registerWithEmail(mapToJson(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
    assertThat(registrationEmailConfirmationStateRepo.count(), is(0L));
  }

  @Test
  public void registerWithEmail_valueNotProvided_shouldNotSucceed() throws Exception {
    EmailRegistrationRequestBody body = EmailRegistrationRequestBody.builder()
        .email("test_email@test.com")
//        .name("test_name")
        .password("test_password")
        .build();

    EmailRegistrationResponse expectedResponse = new EmailRegistrationResponse();
    expectedResponse
        .setError(new Error(BadRequestException.REQUIRED_FIELDS_NOT_GIVEN_ERROR));
    assertThat(registrationEmailConfirmationStateRepo.count(), is(0L));
    registerWithEmail(mapToJson(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
    assertThat(registrationEmailConfirmationStateRepo.count(), is(0L));
  }

  @Test
  public void registerWithEmail_emailConfirmationSentAndStillPending_shouldNotSucceed()
      throws Exception {
    EmailRegistrationRequestBody body = EmailRegistrationRequestBody.builder()
        .email("test_email@test.com")
        .name("test_name")
        .password("test_password")
        .build();

    EmailRegistrationResponse expectedResponse1 = new EmailRegistrationResponse();
    assertThat(registrationEmailConfirmationStateRepo.count(), is(0L));
    registerWithEmail(mapToJson(body))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse1)));
    assertThat(registrationEmailConfirmationStateRepo.count(), is(1L));

    EmailRegistrationResponse expectedResponse2 = new EmailRegistrationResponse();
    expectedResponse2
        .setError(new Error(EmailRegistrationResponse.PIN_ALREADY_SENT_TO_USER_ERROR));
    registerWithEmail(mapToJson(body))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse2)));

    assertThat(registrationEmailConfirmationStateRepo.count(), is(1L));
  }

  // TODO(omaryasser): Add test for user registered with same email after adding a CL that
  //  implements user verification after receiving pin.
  // TODO(omaryasser): Add test for user registered with same email using facebook.

  private ResultActions registerWithEmail(String body) throws Exception {
    return performGetRequest(
        /*user=*/null,
        AuthenticationController.REGISTER_WITH_EMAIL_PATH,
        body);
  }
}
