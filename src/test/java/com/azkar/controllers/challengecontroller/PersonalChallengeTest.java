package com.azkar.controllers.challengecontroller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.TestBase;
import com.azkar.entities.Challenge;
import com.azkar.entities.Challenge.SubChallenges;
import com.azkar.entities.User;
import com.azkar.factories.entities.UserFactory;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.challengecontroller.requests.AddPersonalChallengeRequest;
import com.azkar.payload.challengecontroller.responses.AddPersonalChallengeResponse;
import com.azkar.payload.exceptions.BadRequestException;
import com.azkar.repos.UserRepo;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.results.ResultMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

public class PersonalChallengeTest extends TestBase {

  private static final User USER = UserFactory.getNewUser();
  private static final String CHALLENGE_MOTIVATION = "test-motivation";
  private static final int DATE_OFFSET_IN_SECONDS = 60 * 60;
  private static final ImmutableList<SubChallenges> SUB_CHALLENGES = ImmutableList.of(
      SubChallenges.builder()
          .zekr("test-zekr")
          .originalRepetitions(4)
          .leftRepetitions(4)
          .build());
  private static final String CHALLENGE_NAME = "test-challenge";

  @Autowired
  UserRepo userRepo;

  @Before
  public void before() {
    addNewUser(USER);
  }

  @Test
  // TODO(issue#85): Remove this ignore after making sure this test passes.
  @Ignore
  public void addPersonalChallenge_normalScenario_shouldSucceed() throws Exception {
    AddPersonalChallengeRequest requestBody = AddPersonalChallengeRequest.builder()
        .name(CHALLENGE_NAME)
        .subChallenges(SUB_CHALLENGES)
        .expiryDate(Instant.now().getEpochSecond() + DATE_OFFSET_IN_SECONDS)
        .motivation(CHALLENGE_MOTIVATION).build();

    MvcResult result = performPostRequest(USER, "/challenges/personal", mapToJson(requestBody))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    User user = userRepo.findById(USER.getId()).get();
    assertThat(user.getPersonalChallenges().size(), is(1));
    Challenge expectedChallenge = user.getPersonalChallenges().get(0);
    AddPersonalChallengeResponse expectedResponse = new AddPersonalChallengeResponse();
    expectedResponse.setData(expectedChallenge);
    String actualResponse = result.getResponse().getContentAsString();
    assertThat(actualResponse, equalTo(content().json(mapToJson(expectedResponse))));
  }

  @Test
  public void addPersonalChallenge_pastExpiryDate_shouldFail() throws Exception {
    AddPersonalChallengeRequest requestBody = AddPersonalChallengeRequest.builder()
        .name(CHALLENGE_NAME)
        .subChallenges(SUB_CHALLENGES)
        .expiryDate(Instant.now().getEpochSecond() - DATE_OFFSET_IN_SECONDS)
        .motivation(CHALLENGE_MOTIVATION).build();
    AddPersonalChallengeResponse expectedResponse = new AddPersonalChallengeResponse();
    expectedResponse.setError(new Error(AddPersonalChallengeRequest.PAST_EXPIRY_DATE_ERROR));

    performPostRequest(USER, "/challenges/personal", mapToJson(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
  }

  @Test
  public void addPersonalChallenge_challengeMissingMotivationField_shouldFail() throws Exception {
    AddPersonalChallengeRequest requestBody = AddPersonalChallengeRequest.builder()
        .name(CHALLENGE_NAME)
        .subChallenges(SUB_CHALLENGES)
        .expiryDate(Instant.now().getEpochSecond() + DATE_OFFSET_IN_SECONDS).build();
    AddPersonalChallengeResponse expectedResponse = new AddPersonalChallengeResponse();
    expectedResponse.setError(new Error(BadRequestException.REQUIRED_FIELDS_NOT_GIVEN_ERROR));

    performPostRequest(USER, "/challenges/personal", mapToJson(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
  }
}
