package com.azkar.controllers.authenticationcontroller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.TestBase;
import com.azkar.controllers.utils.JsonHandler;
import com.azkar.entities.User;
import com.azkar.factories.entities.UserFactory;
import com.azkar.payload.ResponseBase.Status;
import com.azkar.payload.authenticationcontroller.responses.ResetPasswordResponse;
import com.azkar.repos.UserRepo;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

public class ResetPasswordTest extends TestBase {

  /**
   * TODO: 1. Have /reset_password to save the password token [DONE]
   * TODO: 2. Send email with the reset password token
   * TODO: 3. Have /verify_password_token to check the token [DONE]
   * TODO: 4. Have /set_password to set the new password
   */
  @Autowired
  UserRepo userRepo;

  User user;

  @Before
  public void setUp() {
    user = UserFactory.getNewUser();
    addNewUser(user);
  }

  @Test
  public void resetPassword_validUser_shouldSaveToken() throws Exception {

    azkarApi.resetPassword(user.getEmail()).andExpect(status().isOk());

    User userFromDb = userRepo.findByEmail(user.getEmail()).get();
    assertThat(userFromDb.getResetPasswordToken(), Matchers.notNullValue());
    assertThat(userFromDb.getResetPasswordToken(), not(emptyString()));
    assertSetPasswordTokenIsNotIncludedInUserResponse(user);
  }

  private void assertSetPasswordTokenIsNotIncludedInUserResponse(User user) throws Exception {
    String userResponseFromApi = azkarApi.searchForUserByUsername(user, user.getUsername())
        .andReturn().getResponse().getContentAsString();
    assertThat(userResponseFromApi, not(containsStringIgnoringCase("resetPasswordToken")));
  }

  @Test
  public void resetPassword_invalidEmail_shouldFail() throws Exception {
    ResetPasswordResponse expectedResponse = new ResetPasswordResponse();
    expectedResponse.setStatus(new Status(Status.USER_NOT_FOUND_ERROR));
    azkarApi.resetPassword("invalidEmail@example.com").andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void validatePasswordToken_validToken_shouldSucceed() throws Exception {
    azkarApi.resetPassword(user.getEmail()).andExpect(status().isOk());
    User userFromDb = userRepo.findByEmail(user.getEmail()).get();
    String token = userFromDb.getResetPasswordToken();

    azkarApi.verifyResetPasswordToken(token).andExpect(status().isOk());
  }

  @Test
  public void validatePasswordToken_validToken_shouldFail() throws Exception {
    azkarApi.resetPassword(user.getEmail()).andExpect(status().isOk());
    ResetPasswordResponse expectedResponse = new ResetPasswordResponse();
    expectedResponse.setStatus(new Status(Status.INVALID_RESET_PASSWORD_TOKEN));

    azkarApi.verifyResetPasswordToken("invalid_token").andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }
}
