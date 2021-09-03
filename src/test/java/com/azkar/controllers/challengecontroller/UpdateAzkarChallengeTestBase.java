package com.azkar.controllers.challengecontroller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.TestBase;
import com.azkar.controllers.utils.JsonHandler;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.entities.Zekr;
import com.azkar.entities.challenges.AzkarChallenge;
import com.azkar.entities.challenges.AzkarChallenge.SubChallenge;
import com.azkar.factories.entities.ChallengeFactory;
import com.azkar.factories.entities.UserFactory;
import com.azkar.payload.ResponseBase.Status;
import com.azkar.payload.challengecontroller.requests.UpdateChallengeRequest;
import com.azkar.payload.challengecontroller.responses.UpdateChallengeResponse;
import com.azkar.repos.GroupRepo;
import com.azkar.repos.UserRepo;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

public abstract class UpdateAzkarChallengeTestBase extends TestBase {

  public static final int OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS =
      ChallengeFactory.azkarSubChallenge1()
          .getRepetitions();
  public static final int NEW_SUB_CHALLENGE_1_LEFT_REPETITIONS =
      OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS - 1;
  public static final int OLD_SUB_CHALLENGE_2_LEFT_REPETITIONS =
      ChallengeFactory.azkarSubChallenge2()
          .getRepetitions();
  public static final int NEW_SUB_CHALLENGE_2_LEFT_REPETITIONS =
      OLD_SUB_CHALLENGE_2_LEFT_REPETITIONS - 2;
  protected User user;
  @Autowired
  protected GroupRepo groupRepo;
  protected Group group;
  @Autowired
  protected UserRepo userRepo;

  static UpdateChallengeRequest createUpdateChallengeRequest(AzkarChallenge newChallenge) {
    return UpdateChallengeRequest.builder().newChallenge(newChallenge).build();
  }

  @Before
  public void setup() throws Exception {
    user = UserFactory.getNewUser();
    addNewUser(user);
    group = azkarApi.addGroupAndReturn(user, "group_name");
  }

  @Test
  public void updateChallenge_updateOneSubChallenge_shouldSucceed() throws Exception {
    AzkarChallenge challenge = createNewChallenge(user);
    challenge.getSubChallenges().get(1)
        .setRepetitions(NEW_SUB_CHALLENGE_2_LEFT_REPETITIONS);
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(challenge);
    UpdateChallengeResponse expectedResponse = new UpdateChallengeResponse();
    expectedResponse.setStatus(new Status(Status.SUCCESS));

    updateChallenge(user, challenge.getId(), requestBody)
        .andExpect(status().isOk())
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    AzkarChallenge updatedChallenge = getChallengeProgressFromApi(challenge);
    assertThat(updatedChallenge.getSubChallenges().get(0).getRepetitions(), is(
        OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS));
    assertThat(updatedChallenge.getSubChallenges().get(1).getRepetitions(), is(
        NEW_SUB_CHALLENGE_2_LEFT_REPETITIONS));
  }

  @Test
  public void updateChallenge_updateMultipleSubChallenges_shouldSucceed() throws Exception {
    AzkarChallenge challenge = createNewChallenge(user);
    challenge.getSubChallenges().get(0)
        .setRepetitions(NEW_SUB_CHALLENGE_1_LEFT_REPETITIONS);
    challenge.getSubChallenges().get(1)
        .setRepetitions(NEW_SUB_CHALLENGE_2_LEFT_REPETITIONS);
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(challenge);

    updateChallenge(user, challenge.getId(), requestBody)
        .andExpect(status().isOk());

    AzkarChallenge updatedChallenge = getChallengeProgressFromApi(challenge);
    assertThat(updatedChallenge.getSubChallenges().get(0).getRepetitions(),
        is(NEW_SUB_CHALLENGE_1_LEFT_REPETITIONS));
    assertThat(updatedChallenge.getSubChallenges().get(1).getRepetitions(),
        is(NEW_SUB_CHALLENGE_2_LEFT_REPETITIONS));
  }

  @Test
  public void updateChallenge_NegativeLeftRepetitions_shouldUpdateWithZero() throws Exception {
    AzkarChallenge challenge = createNewChallenge(user);
    challenge.getSubChallenges().get(0).setRepetitions(-1);
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(challenge);

    updateChallenge(user, challenge.getId(), requestBody)
        .andExpect(status().isOk());

    AzkarChallenge updatedChallenge = getChallengeProgressFromApi(challenge);
    assertThat(updatedChallenge.getSubChallenges().get(0).getRepetitions(),
        is(0));
    assertThat(updatedChallenge.getSubChallenges().get(1).getRepetitions(),
        is(OLD_SUB_CHALLENGE_2_LEFT_REPETITIONS));

  }

  @Test
  public void updateChallenge_IncrementLeftRepetitions_shouldFail() throws Exception {
    AzkarChallenge challenge = createNewChallenge(user);
    challenge.getSubChallenges().get(0)
        .setRepetitions(OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS + 1);
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(challenge);
    UpdateChallengeResponse expectedResponse = new UpdateChallengeResponse();
    expectedResponse
        .setStatus(new Status(Status.INCREMENTING_LEFT_REPETITIONS_ERROR));

    updateChallenge(user, challenge.getId(), requestBody)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    AzkarChallenge updatedChallenge = getChallengeProgressFromApi(challenge);
    assertThat(updatedChallenge.getSubChallenges().get(0).getRepetitions(),
        is(OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS));
    assertThat(updatedChallenge.getSubChallenges().get(1).getRepetitions(),
        is(OLD_SUB_CHALLENGE_2_LEFT_REPETITIONS));

  }

  @Test
  public void updateChallenge_invalidZekrId_shouldFail() throws Exception {
    int invalidZekrId = 777;
    AzkarChallenge challenge = createNewChallenge(user);
    challenge.getSubChallenges().get(1).setZekr(Zekr.builder().id(invalidZekrId).build());
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(challenge);
    UpdateChallengeResponse expectedResponse = new UpdateChallengeResponse();
    expectedResponse
        .setStatus(new Status(Status.NON_EXISTENT_SUB_CHALLENGE_ERROR));

    updateChallenge(user, challenge.getId(), requestBody)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    AzkarChallenge updatedChallenge = getChallengeProgressFromApi(challenge);
    assertThat(updatedChallenge.getSubChallenges().get(0).getRepetitions(),
        is(OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS));
    assertThat(updatedChallenge.getSubChallenges().get(1).getRepetitions(),
        is(OLD_SUB_CHALLENGE_2_LEFT_REPETITIONS));

  }

  @Test
  public void updateChallenge_invalidChallengeId_shouldFail() throws Exception {
    AzkarChallenge unsavedChallenge = ChallengeFactory.getNewChallenge("invalidId");
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(unsavedChallenge);
    UpdateChallengeResponse response = new UpdateChallengeResponse();
    response.setStatus(new Status(Status.CHALLENGE_NOT_FOUND_ERROR));

    updateChallenge(user, "invalidId", requestBody)
        .andExpect(status().isBadRequest())
        .andExpect(content().json(JsonHandler.toJson(response)));
  }

  @Test
  public void updateChallenge_extraSubChallenge_shouldFail() throws Exception {
    AzkarChallenge challenge = createNewChallenge(user);
    List<SubChallenge> updatedSubChallenges = ImmutableList.of(
        challenge.getSubChallenges().get(0),
        challenge.getSubChallenges().get(0),
        challenge.getSubChallenges().get(1));
    updatedSubChallenges.get(2).setRepetitions(NEW_SUB_CHALLENGE_2_LEFT_REPETITIONS);
    challenge.setSubChallenges(updatedSubChallenges);
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(challenge);
    UpdateChallengeResponse response = new UpdateChallengeResponse();
    response.setStatus(new Status(Status.MISSING_OR_DUPLICATED_SUB_CHALLENGE_ERROR));

    updateChallenge(user, challenge.getId(), requestBody)
        .andExpect(status().isBadRequest())
        .andExpect(content().json(JsonHandler.toJson(response)));

    AzkarChallenge updatedChallenge = getChallengeProgressFromApi(challenge);
    assertThat(updatedChallenge.getSubChallenges().get(0).getRepetitions(), is(
        OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS));
    assertThat(updatedChallenge.getSubChallenges().get(1).getRepetitions(), is(
        OLD_SUB_CHALLENGE_2_LEFT_REPETITIONS));
  }

  @Test
  public void updateChallenge_missingSubChallenge_shouldFail() throws Exception {
    AzkarChallenge challenge = createNewChallenge(user);
    List<SubChallenge> updatedSubChallenges = ImmutableList.of(
        challenge.getSubChallenges().get(0));
    updatedSubChallenges.get(0).setRepetitions(NEW_SUB_CHALLENGE_1_LEFT_REPETITIONS);
    challenge.setSubChallenges(updatedSubChallenges);
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(challenge);
    UpdateChallengeResponse response = new UpdateChallengeResponse();
    response.setStatus(new Status(Status.MISSING_OR_DUPLICATED_SUB_CHALLENGE_ERROR));

    updateChallenge(user, challenge.getId(), requestBody)
        .andExpect(status().isBadRequest())
        .andExpect(content().json(JsonHandler.toJson(response)));

    AzkarChallenge updatedChallenge = getChallengeProgressFromApi(challenge);
    assertThat(updatedChallenge.getSubChallenges().get(0).getRepetitions(), is(
        OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS));
    assertThat(updatedChallenge.getSubChallenges().get(1).getRepetitions(), is(
        OLD_SUB_CHALLENGE_2_LEFT_REPETITIONS));
  }

  @Test
  public void updateChallenge_duplicatedSubChallenge_shouldFail() throws Exception {
    AzkarChallenge challenge = createNewChallenge(user);
    List<SubChallenge> updatedSubChallenges = ImmutableList.of(
        challenge.getSubChallenges().get(0),
        challenge.getSubChallenges().get(0));
    updatedSubChallenges.get(0).setRepetitions(NEW_SUB_CHALLENGE_1_LEFT_REPETITIONS);
    updatedSubChallenges.get(1).setRepetitions(NEW_SUB_CHALLENGE_1_LEFT_REPETITIONS);
    challenge.setSubChallenges(updatedSubChallenges);
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(challenge);
    UpdateChallengeResponse response = new UpdateChallengeResponse();
    response.setStatus(new Status(Status.MISSING_OR_DUPLICATED_SUB_CHALLENGE_ERROR));

    updateChallenge(user, challenge.getId(), requestBody)
        .andExpect(status().isBadRequest())
        .andExpect(content().json(JsonHandler.toJson(response)));

    AzkarChallenge updatedChallenge = getChallengeProgressFromApi(challenge);
    assertThat(updatedChallenge.getSubChallenges().get(0).getRepetitions(), is(
        OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS));
    assertThat(updatedChallenge.getSubChallenges().get(1).getRepetitions(), is(
        OLD_SUB_CHALLENGE_2_LEFT_REPETITIONS));
  }

  protected abstract AzkarChallenge createNewChallenge(User user) throws Exception;

  protected abstract ResultActions updateChallenge(User user, String challengeId,
      UpdateChallengeRequest requestBody) throws Exception;

  protected abstract AzkarChallenge getChallengeProgressFromApi(AzkarChallenge challenge)
      throws Exception;
}
