package com.azkar.controllers.challengecontroller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.ControllerTestBase;
import com.azkar.entities.Challenge;
import com.azkar.entities.Challenge.SubChallenges;
import com.azkar.entities.User;
import com.azkar.factories.UserFactory;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.challengecontroller.AddPersonalChallengeRequest;
import com.azkar.payload.challengecontroller.AddPersonalChallengeResponse;
import com.azkar.payload.exceptions.BadRequestException;
import com.azkar.repos.UserRepo;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class PersonalChallengeTest extends ControllerTestBase {

  private static final User USER = UserFactory.getNewUser();
  private static final long PAST_DATE = Instant.now().getEpochSecond() - 3600;
  private static final long FUTURE_DATE = Instant.now().getEpochSecond() + 3600;
  private static final ImmutableList<SubChallenges> SUB_CHALLENGES = ImmutableList.of(
      new SubChallenges(
          /* zekr= */"test-zekr", /* originalRepetitions= */4, /* leftRepetitions= */4));
  private static final AddPersonalChallengeRequest BASE_ADD_PERSONAL_CHALLENGE_REQUEST =
      AddPersonalChallengeRequest.builder()
          .name("test-challenge")
          .subChallenges(SUB_CHALLENGES)
          .build();

  @Autowired
  UserRepo userRepo;

  @Before
  public void before() {
    addNewUser(USER);
  }

  @Test
  public void addPersonalChallenge_normalScenario_shouldSucceed() throws Exception {
    AddPersonalChallengeRequest requestBody = BASE_ADD_PERSONAL_CHALLENGE_REQUEST.toBuilder()
        .expiryDate(FUTURE_DATE).motivation("test-motivation").build();

    MvcResult result = performPostRequest(USER, "/challenges/personal", mapToJson(requestBody))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    Challenge expectedChallenge = userRepo.findById(USER.getId()).get().getPersonalChallenges()
        .get(0);
    AddPersonalChallengeResponse expectedResponse = new AddPersonalChallengeResponse();
    expectedResponse.setData(expectedChallenge);
    String actualResponse = result.getResponse().getContentAsString();
    assertThat(actualResponse, equalTo(mapToJson(expectedResponse)));
  }

  @Test
  public void addPersonalChallenge_pastExpiryDate_shouldFail() throws Exception {
    AddPersonalChallengeRequest requestBody = BASE_ADD_PERSONAL_CHALLENGE_REQUEST.toBuilder()
        .expiryDate(PAST_DATE).motivation("test-motivation").build();
    AddPersonalChallengeResponse expectedResponse = new AddPersonalChallengeResponse();
    expectedResponse.setError(new Error(AddPersonalChallengeRequest.PAST_EXPIRY_DATE_ERROR));

    performPostRequest(USER, "/challenges/personal", mapToJson(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
  }

  @Test
  public void addPersonalChallenge_missing_shouldFail() throws Exception {
    AddPersonalChallengeRequest requestBody = BASE_ADD_PERSONAL_CHALLENGE_REQUEST.toBuilder()
        .expiryDate(FUTURE_DATE).build();
    AddPersonalChallengeResponse expectedResponse = new AddPersonalChallengeResponse();
    expectedResponse.setError(new Error(BadRequestException.REQUIRED_FIELDS_NOT_GIVEN_ERROR));

    performPostRequest(USER, "/challenges/personal", mapToJson(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
  }
}
