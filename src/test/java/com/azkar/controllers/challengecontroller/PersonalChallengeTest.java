package com.azkar.controllers.challengecontroller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.TestBase;
import com.azkar.controllers.utils.JsonHandler;
import com.azkar.entities.Challenge;
import com.azkar.entities.Challenge.SubChallenge;
import com.azkar.entities.User;
import com.azkar.factories.entities.ChallengeFactory;
import com.azkar.factories.entities.UserFactory;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.challengecontroller.requests.AddPersonalChallengeRequest;
import com.azkar.payload.challengecontroller.responses.AddPersonalChallengeResponse;
import com.azkar.payload.challengecontroller.responses.GetChallengesResponse;
import com.azkar.payload.exceptions.BadRequestException;
import com.azkar.repos.UserRepo;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class PersonalChallengeTest extends TestBase {

  private static final User USER = UserFactory.getNewUser();
  private static final String CHALLENGE_MOTIVATION = ChallengeFactory.CHALLENGE_MOTIVATION;
  private static final long DATE_OFFSET_IN_SECONDS = ChallengeFactory.EXPIRY_DATE_OFFSET;
  private static final ImmutableList<SubChallenge> SUB_CHALLENGES = ImmutableList.of(
      ChallengeFactory.subChallenge1(), ChallengeFactory.subChallenge2());
  private static final String CHALLENGE_NAME = "test-challenge";

  @Autowired
  UserRepo userRepo;

  public static AddPersonalChallengeRequest createPersonalChallengeRequest(long expiryDate) {
    return createPersonalChallengeRequest(CHALLENGE_NAME, expiryDate);
  }

  public static AddPersonalChallengeRequest createPersonalChallengeRequest(String name,
      long expiryDate) {
    return AddPersonalChallengeRequest.builder()
        .name(name)
        .subChallenges(SUB_CHALLENGES)
        .expiryDate(expiryDate)
        .motivation(CHALLENGE_MOTIVATION).build();
  }

  @Before
  public void before() {
    addNewUser(USER);
  }

  @Test
  public void addPersonalChallenge_normalScenario_shouldSucceed() throws Exception {
    long expiryDate = Instant.now().getEpochSecond() + DATE_OFFSET_IN_SECONDS;
    AddPersonalChallengeRequest requestBody = createPersonalChallengeRequest(expiryDate);

    MvcResult result = azkarApi.createPersonalChallenge(USER, requestBody)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    assertThat(userRepo.findById(USER.getId()).get().getPersonalChallenges().size(), is(1));
    Challenge expectedChallenge = Challenge.builder()
        .name(CHALLENGE_NAME)
        .subChallenges(SUB_CHALLENGES)
        .expiryDate(expiryDate)
        .motivation(CHALLENGE_MOTIVATION)
        .creatingUserId(USER.getId())
        .build();
    AddPersonalChallengeResponse expectedResponse = new AddPersonalChallengeResponse();
    expectedResponse.setData(expectedChallenge);
    String actualResponseJson = result.getResponse().getContentAsString();
    JSONAssert.assertEquals(JsonHandler.toJson(expectedResponse), actualResponseJson, /* strict= */
        false);
  }

  @Test
  public void addPersonalChallenge_pastExpiryDate_shouldFail() throws Exception {
    long pastExpiryDate = Instant.now().getEpochSecond() - DATE_OFFSET_IN_SECONDS;
    AddPersonalChallengeRequest requestBody = createPersonalChallengeRequest(pastExpiryDate);
    AddPersonalChallengeResponse expectedResponse = new AddPersonalChallengeResponse();
    expectedResponse.setError(new Error(AddPersonalChallengeRequest.PAST_EXPIRY_DATE_ERROR));

    azkarApi.createPersonalChallenge(USER, requestBody)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void addPersonalChallenge_challengeMissingMotivationField_shouldFail() throws Exception {
    AddPersonalChallengeRequest requestBody = AddPersonalChallengeRequest.builder()
        .name(CHALLENGE_NAME)
        .subChallenges(SUB_CHALLENGES)
        .expiryDate(Instant.now().getEpochSecond() + DATE_OFFSET_IN_SECONDS).build();
    AddPersonalChallengeResponse expectedResponse = new AddPersonalChallengeResponse();
    expectedResponse.setError(new Error(BadRequestException.REQUIRED_FIELDS_NOT_GIVEN_ERROR));

    azkarApi.createPersonalChallenge(USER, requestBody)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void getPersonalChallenge_emptyList_shouldSucceed() throws Exception {
    GetChallengesResponse expectedResponse = new GetChallengesResponse();
    expectedResponse.setData(new ArrayList<>());

    azkarApi.getPersonalChallenges(USER)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void getPersonalChallenge_multipleChallenges_shouldSucceed() throws Exception {
    long expiryDate = Instant.now().getEpochSecond() + ChallengeFactory.EXPIRY_DATE_OFFSET;
    AddPersonalChallengeRequest request1 = PersonalChallengeTest
        .createPersonalChallengeRequest("challenge_1", expiryDate);
    AddPersonalChallengeRequest request2 = PersonalChallengeTest
        .createPersonalChallengeRequest("challenge_2", expiryDate);

    createPersonalChallenge(USER, request1);
    createPersonalChallenge(USER, request2);

    String response = azkarApi.getPersonalChallenges(USER)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn().getResponse().getContentAsString();

    List<Challenge> data = JsonHandler.fromJson(response, GetChallengesResponse.class)
        .getData();
    assertThat(data, hasSize(2));
    assertUserChallengeConsistentWithRequest(data.get(0), request1);
    assertUserChallengeConsistentWithRequest(data.get(1), request2);
  }

  private void assertUserChallengeConsistentWithRequest(
      Challenge challenge,
      AddPersonalChallengeRequest request) {
    assertThat(challenge.getName(), is(request.getName()));
    assertThat(challenge.getCreatingUserId(), is(USER.getId()));
  }
}
