package com.azkar.controllers.authenticationcontroller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.TestBase;
import com.azkar.controllers.AuthenticationController;
import com.azkar.entities.RegistrationEmailConfirmationState;
import com.azkar.entities.User;
import com.azkar.factories.UserFactory;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.authenticationcontroller.requests.EmailRegistrationRequestBody;
import com.azkar.payload.authenticationcontroller.requests.EmailVerificationRequestBody;
import com.azkar.payload.authenticationcontroller.responses.EmailRegistrationResponse;
import com.azkar.payload.authenticationcontroller.responses.EmailVerificationResponse;
import com.azkar.payload.exceptions.BadRequestException;
import com.azkar.repos.RegistrationEmailConfirmationStateRepo;
import com.azkar.repos.UserRepo;
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

  @Autowired
  UserRepo userRepo;

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

  @Test
  public void registerWithEmail_userAlreadyRegistered_shouldNotSucceed()
      throws Exception {
    User user = UserFactory.getNewUser();
    addNewUser(user);

    EmailRegistrationRequestBody body = EmailRegistrationRequestBody.builder()
        .email(user.getEmail())
        .name("example_name")
        .password("example_password")
        .build();
    EmailRegistrationResponse expectedResponse = new EmailRegistrationResponse();
    expectedResponse.setError(new Error(EmailRegistrationResponse.USER_ALREADY_REGISTERED_ERROR));

    assertThat(registrationEmailConfirmationStateRepo.count(), is(0L));
    assertThat(userRepo.count(), is(1L));
    registerWithEmail(mapToJson(body))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
    assertThat(registrationEmailConfirmationStateRepo.count(), is(0L));
    assertThat(userRepo.count(), is(1L));
  }

  @Test
  public void registerWithEmail_userAlreadyRegisteredWithFacebook_shouldNotSucceed()
      throws Exception {
    User registeredWithFacebookUser = UserFactory.getUserRegisteredWithFacebook();
    addNewUser(registeredWithFacebookUser);

    EmailRegistrationRequestBody body = EmailRegistrationRequestBody.builder()
        .email(registeredWithFacebookUser.getUserFacebookData().getEmail())
        .name("example_name")
        .password("example_password")
        .build();
    EmailRegistrationResponse expectedResponse = new EmailRegistrationResponse();
    expectedResponse
        .setError(new Error(EmailRegistrationResponse.USER_ALREADY_REGISTERED_WITH_FACEBOOK));

    assertThat(registrationEmailConfirmationStateRepo.count(), is(0L));
    assertThat(userRepo.count(), is(1L));
    registerWithEmail(mapToJson(body))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
    assertThat(registrationEmailConfirmationStateRepo.count(), is(0L));
    assertThat(userRepo.count(), is(1L));
  }

  @Test
  public void verifyEmail_normalScenario_shouldSucceed() throws Exception {
    final String email = "example_email@example_domain.com";
    final int pin = 123456;
    mockSendingEmailVerificationPin(email, pin);

    assertThat(registrationEmailConfirmationStateRepo.count(), equalTo(1L));
    assertThat(userRepo.count(), equalTo(0L));
    EmailVerificationRequestBody body = EmailVerificationRequestBody.builder()
        .email(email)
        .pin(pin)
        .build();
    EmailVerificationResponse expectedResponse = new EmailVerificationResponse();

    verifyEmail(mapToJson(body))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
    assertThat(registrationEmailConfirmationStateRepo.count(), equalTo(0L));
    assertThat(userRepo.count(), equalTo(1L));
    assertThat("User is added to the database", userRepo.existsByEmail(email));
  }

  @Test
  public void verifyEmail_userAlreadyVerified_shouldNotSucceed() throws Exception {
    final String email = "example_email@example_domain.com";
    final int pin = 123456;
    mockSendingEmailVerificationPin(email, pin);

    EmailVerificationRequestBody body = EmailVerificationRequestBody.builder()
        .email(email)
        .pin(pin)
        .build();
    // Verify for the first time.
    verifyEmail(mapToJson(body)).andExpect(status().isOk());
    EmailVerificationResponse expectedResponse = new EmailVerificationResponse();
    expectedResponse.setError(new Error(EmailVerificationResponse.EMAIL_ALREADY_VERIFIED_ERROR));

    // Verify again
    verifyEmail(mapToJson(body))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
    assertThat(registrationEmailConfirmationStateRepo.count(), equalTo(0L));
    assertThat(userRepo.count(), equalTo(1L));
  }

  @Test
  public void verifyEmail_wrongEmail_shouldNotSucceed() throws Exception {
    final String correctEmail = "example_email1@example_domain.com";
    final String wrongEmail = "example_email2@example_domain.com";
    final int pin = 123456;
    mockSendingEmailVerificationPin(correctEmail, pin);

    assertThat(registrationEmailConfirmationStateRepo.count(), equalTo(1L));
    assertThat(userRepo.count(), equalTo(0L));
    EmailVerificationRequestBody body = EmailVerificationRequestBody.builder()
        .email(wrongEmail)
        .pin(pin)
        .build();
    EmailVerificationResponse expectedResponse = new EmailVerificationResponse();
    expectedResponse.setError(new Error(EmailVerificationResponse.VERIFICATION_ERROR));

    verifyEmail(mapToJson(body))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
    assertThat(registrationEmailConfirmationStateRepo.count(), equalTo(1L));
    assertThat(userRepo.count(), equalTo(0L));
  }

  @Test
  public void verifyEmail_wrongPin_shouldNotSucceed() throws Exception {
    final String email = "example_email1@example_domain.com";
    final int correctPin = 123456;
    final int wrongPin = 111111;
    mockSendingEmailVerificationPin(email, correctPin);

    assertThat(registrationEmailConfirmationStateRepo.count(), equalTo(1L));
    assertThat(userRepo.count(), equalTo(0L));
    EmailVerificationRequestBody body = EmailVerificationRequestBody.builder()
        .email(email)
        .pin(wrongPin)
        .build();
    EmailVerificationResponse expectedResponse = new EmailVerificationResponse();
    expectedResponse.setError(new Error(EmailVerificationResponse.VERIFICATION_ERROR));

    verifyEmail(mapToJson(body))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
    assertThat(registrationEmailConfirmationStateRepo.count(), equalTo(1L));
    assertThat(userRepo.count(), equalTo(0L));
  }

  private void mockSendingEmailVerificationPin(String email, int pin) {
    registrationEmailConfirmationStateRepo.insert(
        RegistrationEmailConfirmationState.builder()
            .name("example_name")
            .email(email)
            .password("example_password")
            .pin(pin)
            .build());
  }

  private ResultActions registerWithEmail(String body) throws Exception {
    return performGetRequest(
        /*user=*/null,
        AuthenticationController.REGISTER_WITH_EMAIL_PATH,
        body);
  }

  private ResultActions verifyEmail(String body) throws Exception {
    return performGetRequest(
        /*user=*/null,
        AuthenticationController.VERIFY_EMAIL_PATH,
        body);
  }
}
