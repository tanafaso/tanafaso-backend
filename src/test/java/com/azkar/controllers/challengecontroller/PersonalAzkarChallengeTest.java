package com.azkar.controllers.challengecontroller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.TestBase;
import com.azkar.controllers.utils.JsonHandler;
import com.azkar.entities.User;
import com.azkar.entities.Zekr;
import com.azkar.entities.challenges.AzkarChallenge;
import com.azkar.entities.challenges.AzkarChallenge.SubChallenge;
import com.azkar.factories.entities.ChallengeFactory;
import com.azkar.factories.entities.UserFactory;
import com.azkar.payload.ResponseBase.Status;
import com.azkar.payload.challengecontroller.requests.AddPersonalChallengeRequest;
import com.azkar.payload.challengecontroller.responses.AddAzkarChallengeResponse;
import com.azkar.payload.challengecontroller.responses.AddPersonalChallengeResponse;
import com.azkar.payload.challengecontroller.responses.DeleteChallengeResponse;
import com.azkar.payload.challengecontroller.responses.GetChallengeResponse;
import com.azkar.payload.challengecontroller.responses.GetChallengesResponse;
import com.azkar.repos.AzkarChallengeRepo;
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

public class PersonalAzkarChallengeTest extends TestBase {

  private static final User USER = UserFactory.getNewUser();
  private static final String CHALLENGE_MOTIVATION = ChallengeFactory.CHALLENGE_MOTIVATION;
  private static final long DATE_OFFSET_IN_SECONDS = ChallengeFactory.EXPIRY_DATE_OFFSET;
  private static final ImmutableList<SubChallenge> SUB_CHALLENGES = ImmutableList.of(
      ChallengeFactory.subChallenge1(), ChallengeFactory.subChallenge2());
  private static final String CHALLENGE_NAME = "test-challenge";

  @Autowired
  UserRepo userRepo;

  @Autowired
  AzkarChallengeRepo challengeRepo;

  public static AddPersonalChallengeRequest createPersonalChallengeRequest(long expiryDate) {
    return createPersonalChallengeRequest(CHALLENGE_NAME, expiryDate);
  }

  public static AddPersonalChallengeRequest createPersonalChallengeRequest(String name,
      long expiryDate) {
    AzkarChallenge challenge =
        AzkarChallenge.builder().name(name).subChallenges(SUB_CHALLENGES).expiryDate(expiryDate)
            .motivation(CHALLENGE_MOTIVATION).build();
    return AddPersonalChallengeRequest.addPersonalChallengeRequestBuilder()
        .challenge(challenge).build();
  }

  @Before
  public void before() {
    addNewUser(USER);
  }

  @Test
  public void addPersonalChallenge_normalScenario_shouldSucceed() throws Exception {
    long expiryDate = Instant.now().getEpochSecond() + DATE_OFFSET_IN_SECONDS;
    AddPersonalChallengeRequest requestBody = createPersonalChallengeRequest(expiryDate);

    long allChallengesCountBeforeRequest = challengeRepo.count();

    MvcResult result = azkarApi.addPersonalChallenge(USER, requestBody)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    assertThat(challengeRepo.count(), is(allChallengesCountBeforeRequest + 1));
    assertThat(userRepo.findById(USER.getId()).get().getPersonalChallenges().size(), is(1));
    AzkarChallenge expectedChallenge = AzkarChallenge.builder()
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
  public void addChallenge_duplicateZekr_shouldNotSucceed() throws Exception {
    long expiryDate = Instant.now().getEpochSecond() + ChallengeFactory.EXPIRY_DATE_OFFSET;
    AddPersonalChallengeRequest requestBody = createPersonalChallengeRequest(expiryDate);

    SubChallenge subChallenge1 =
        SubChallenge.builder().repetitions(2).zekr(Zekr.builder().id(1).zekr("zekr").build())
            .build();
    SubChallenge subChallenge2 =
        SubChallenge.builder().repetitions(3).zekr(Zekr.builder().id(1).zekr("zekr").build())
            .build();
    requestBody.getChallenge().setSubChallenges(ImmutableList.of(subChallenge1, subChallenge2));
    AddAzkarChallengeResponse expectedResponse = new AddAzkarChallengeResponse();
    expectedResponse.setStatus(new Status(Status.CHALLENGE_CREATION_DUPLICATE_ZEKR_ERROR));

    azkarApi.addPersonalChallenge(USER, requestBody)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    List<AzkarChallenge> challengesProgress = userRepo.findById(USER.getId()).get()
        .getPersonalChallenges();
    assertTrue("Challenges progress list is not empty.", challengesProgress.isEmpty());
  }

  @Test
  public void addPersonalChallenge_pastExpiryDate_shouldFail() throws Exception {
    long pastExpiryDate = Instant.now().getEpochSecond() - DATE_OFFSET_IN_SECONDS;
    AddPersonalChallengeRequest requestBody = createPersonalChallengeRequest(pastExpiryDate);
    AddPersonalChallengeResponse expectedResponse = new AddPersonalChallengeResponse();
    expectedResponse.setStatus(new Status(Status.PAST_EXPIRY_DATE_ERROR));

    azkarApi.addPersonalChallenge(USER, requestBody)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void addPersonalChallenge_challengeMissingMotivationField_shouldSucceed()
      throws Exception {
    AzkarChallenge challenge =
        AzkarChallenge.builder().name(CHALLENGE_NAME).subChallenges(SUB_CHALLENGES)
            .expiryDate(Instant.now().getEpochSecond() + DATE_OFFSET_IN_SECONDS).build();
    AddPersonalChallengeRequest requestBody =
        AddPersonalChallengeRequest.addPersonalChallengeRequestBuilder()
            .challenge(challenge).build();

    AzkarChallenge expectedChallenge = challenge.toBuilder()
        .creatingUserId(USER.getId())
        .build();
    AddPersonalChallengeResponse expectedResponse = new AddPersonalChallengeResponse();
    expectedResponse.setData(expectedChallenge);
    MvcResult result = azkarApi.addPersonalChallenge(USER, requestBody)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    assertThat(userRepo.findById(USER.getId()).get().getPersonalChallenges().size(), is(1));
    String actualResponseJson = result.getResponse().getContentAsString();
    JSONAssert.assertEquals(JsonHandler.toJson(expectedResponse), actualResponseJson, /* strict= */
        false);
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
    AddPersonalChallengeRequest request1 = PersonalAzkarChallengeTest
        .createPersonalChallengeRequest("challenge_1", expiryDate);
    AddPersonalChallengeRequest request2 = PersonalAzkarChallengeTest
        .createPersonalChallengeRequest("challenge_2", expiryDate);

    createPersonalChallenge(USER, request1);
    createPersonalChallenge(USER, request2);

    String response = azkarApi.getPersonalChallenges(USER)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn().getResponse().getContentAsString();

    List<AzkarChallenge> data = JsonHandler.fromJson(response, GetChallengesResponse.class)
        .getData();
    assertThat(data, hasSize(2));
    assertUserChallengeConsistentWithRequest(data.get(0), request2);
    assertUserChallengeConsistentWithRequest(data.get(1), request1);
  }

  @Test
  public void getOriginalChallenge_normalScenario_shouldSucceed() throws Exception {
    AzkarChallenge queriedChallenge = createPersonalChallenge(USER);

    // Change the user's copy of the challenge
    User updatedUser1 = userRepo.findById(USER.getId()).get();
    updatedUser1.getPersonalChallenges().stream().forEach(
        userChallenge -> userChallenge.getSubChallenges().stream().forEach(
            subChallenge -> subChallenge.setRepetitions(subChallenge.getRepetitions() + 1)
        )
    );
    userRepo.save(updatedUser1);

    GetChallengeResponse response = new GetChallengeResponse();
    response.setData(queriedChallenge);

    azkarApi.getOriginalChallenge(USER, queriedChallenge.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(response)));
  }

  @Test
  public void deletePersonalChallenge_normalScenario_shouldSucceed() throws Exception {
    AzkarChallenge queriedChallenge = createPersonalChallenge(USER);
    AzkarChallenge anotherChallenge = createPersonalChallenge(USER);
    long numOfAllChallengesBeforeRequest = challengeRepo.count();

    DeleteChallengeResponse response = new DeleteChallengeResponse();
    response.setData(queriedChallenge);
    List<AzkarChallenge> userPersonalChallenges =
        userRepo.findById(USER.getId()).get().getPersonalChallenges();
    assertThat(userPersonalChallenges.size(), is(2));

    azkarApi.deletePersonalChallenge(USER, queriedChallenge.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(response)));

    assertThat(challengeRepo.count(), is(numOfAllChallengesBeforeRequest - 1));
    userPersonalChallenges = userRepo.findById(USER.getId()).get().getPersonalChallenges();
    assertThat(userPersonalChallenges.size(), is(1));
    assertThat(userPersonalChallenges.get(0).getId(), equalTo(anotherChallenge.getId()));
  }


  @Test
  public void deletePersonalChallenge_invalidChallengeId_shouldFail() throws Exception {
    DeleteChallengeResponse notFoundResponse = new DeleteChallengeResponse();
    notFoundResponse.setStatus(new Status(Status.CHALLENGE_NOT_FOUND_ERROR));

    azkarApi.deletePersonalChallenge(USER, "invalidChallengeId")
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(notFoundResponse)));
  }

  @Test
  public void deletePersonalChallenge_userDoesNotHaveChallenge_shouldFail() throws Exception {
    AzkarChallenge challenge = createPersonalChallenge(USER);
    User anotherUser = UserFactory.getNewUser();
    addNewUser(anotherUser);
    DeleteChallengeResponse notFoundResponse = new DeleteChallengeResponse();
    notFoundResponse.setStatus(new Status(Status.CHALLENGE_NOT_FOUND_ERROR));

    azkarApi.deletePersonalChallenge(anotherUser, challenge.getId())
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(notFoundResponse)));
  }

  private void assertUserChallengeConsistentWithRequest(
      AzkarChallenge challenge,
      AddPersonalChallengeRequest request) {
    assertThat(challenge.getName(), is(request.getChallenge().getName()));
    assertThat(challenge.getCreatingUserId(), is(USER.getId()));
  }
}
