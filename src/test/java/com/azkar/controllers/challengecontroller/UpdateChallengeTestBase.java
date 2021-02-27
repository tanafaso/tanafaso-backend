package com.azkar.controllers.challengecontroller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.TestBase;
import com.azkar.controllers.utils.JsonHandler;
import com.azkar.entities.Challenge;
import com.azkar.entities.Challenge.SubChallenge;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.entities.User.ChallengeProgress;
import com.azkar.factories.entities.ChallengeFactory;
import com.azkar.factories.entities.GroupFactory;
import com.azkar.factories.entities.UserFactory;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.challengecontroller.requests.UpdateChallengeRequest;
import com.azkar.payload.challengecontroller.responses.UpdateChallengeResponse;
import com.azkar.repos.GroupRepo;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

public abstract class UpdateChallengeTestBase extends TestBase {

  public static final int OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS = ChallengeFactory.subChallenge1()
      .getOriginalRepetitions();
  public static final int NEW_SUB_CHALLENGE_1_LEFT_REPETITIONS =
      OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS - 1;
  public static final int OLD_SUB_CHALLENGE_2_LEFT_REPETITIONS = ChallengeFactory.subChallenge2()
      .getOriginalRepetitions();
  public static final int NEW_SUB_CHALLENGE_2_LEFT_REPETITIONS =
      OLD_SUB_CHALLENGE_2_LEFT_REPETITIONS - 2;
  protected User user;
  @Autowired
  protected GroupRepo groupRepo;
  protected Group group;

  static UpdateChallengeRequest createUpdateChallengeRequest(Challenge newChallenge) {
    return UpdateChallengeRequest.builder().newChallenge(newChallenge).build();
  }

  @Before
  public void setup() {
    user = UserFactory.getNewUser();
    addNewUser(user);
    group = GroupFactory.getNewGroup(user.getId());
    groupRepo.save(group);
  }

  @Test
  public void updateChallenge_updateOneSubChallenge_shouldSucceed() throws Exception {
    Challenge challenge = createNewChallenge(user);
    challenge.getSubChallenges().get(1)
        .setOriginalRepetitions(NEW_SUB_CHALLENGE_2_LEFT_REPETITIONS);
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(challenge);

    updateChallenge(user, challenge.getId(), requestBody)
        .andExpect(status().isOk());

    ChallengeProgress updatedChallenge = getChallengeProgressFromApi(challenge);
    assertThat(updatedChallenge.getSubChallenges().get(0).getLeftRepetitions(), is(
        OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS));
    assertThat(updatedChallenge.getSubChallenges().get(1).getLeftRepetitions(), is(
        NEW_SUB_CHALLENGE_2_LEFT_REPETITIONS));

  }

  @Test
  public void updateChallenge_updateMultipleSubChallenges_shouldSucceed() throws Exception {
    Challenge challenge = createNewChallenge(user);
    challenge.getSubChallenges().get(0)
        .setOriginalRepetitions(NEW_SUB_CHALLENGE_1_LEFT_REPETITIONS);
    challenge.getSubChallenges().get(1)
        .setOriginalRepetitions(NEW_SUB_CHALLENGE_2_LEFT_REPETITIONS);
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(challenge);

    updateChallenge(user, challenge.getId(), requestBody)
        .andExpect(status().isOk());

    ChallengeProgress updatedChallenge = getChallengeProgressFromApi(challenge);
    assertThat(updatedChallenge.getSubChallenges().get(0).getLeftRepetitions(),
        is(NEW_SUB_CHALLENGE_1_LEFT_REPETITIONS));
    assertThat(updatedChallenge.getSubChallenges().get(1).getLeftRepetitions(),
        is(NEW_SUB_CHALLENGE_2_LEFT_REPETITIONS));
  }

  @Test
  public void updateChallenge_NegativeLeftRepetitions_shouldUpdateWithZero() throws Exception {
    Challenge challenge = createNewChallenge(user);
    challenge.getSubChallenges().get(0).setOriginalRepetitions(-1);
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(challenge);

    updateChallenge(user, challenge.getId(), requestBody)
        .andExpect(status().isOk());

    ChallengeProgress updatedChallenge = getChallengeProgressFromApi(challenge);
    assertThat(updatedChallenge.getSubChallenges().get(0).getLeftRepetitions(),
        is(0));
    assertThat(updatedChallenge.getSubChallenges().get(1).getLeftRepetitions(),
        is(OLD_SUB_CHALLENGE_2_LEFT_REPETITIONS));

  }

  @Test
  public void updateChallenge_IncrementLeftRepetitions_shouldFail() throws Exception {
    Challenge challenge = createNewChallenge(user);
    challenge.getSubChallenges().get(0)
        .setOriginalRepetitions(OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS + 1);
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(challenge);
    UpdateChallengeResponse expectedResponse = new UpdateChallengeResponse();
    expectedResponse
        .setError(new Error(UpdateChallengeResponse.INCREMENTING_LEFT_REPETITIONS_ERROR));

    updateChallenge(user, challenge.getId(), requestBody)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    ChallengeProgress updatedChallenge = getChallengeProgressFromApi(challenge);
    assertThat(updatedChallenge.getSubChallenges().get(0).getLeftRepetitions(),
        is(OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS));
    assertThat(updatedChallenge.getSubChallenges().get(1).getLeftRepetitions(),
        is(OLD_SUB_CHALLENGE_2_LEFT_REPETITIONS));

  }

  @Test
  public void updateChallenge_invalidZekrId_shouldFail() throws Exception {
    String invalidZekrId = "777";
    Challenge challenge = createNewChallenge(user);
    challenge.getSubChallenges().get(1).setZekrId(invalidZekrId);
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(challenge);
    UpdateChallengeResponse expectedResponse = new UpdateChallengeResponse();
    expectedResponse
        .setError(new Error(UpdateChallengeResponse.NON_EXISTENT_SUB_CHALLENGE_ERROR));

    updateChallenge(user, challenge.getId(), requestBody)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    ChallengeProgress updatedChallenge = getChallengeProgressFromApi(challenge);
    assertThat(updatedChallenge.getSubChallenges().get(0).getLeftRepetitions(),
        is(OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS));
    assertThat(updatedChallenge.getSubChallenges().get(1).getLeftRepetitions(),
        is(OLD_SUB_CHALLENGE_2_LEFT_REPETITIONS));

  }

  @Test
  public void updateChallenge_invalidChallengeId_shouldFail() throws Exception {
    Challenge unsavedChallenge = ChallengeFactory.getNewChallenge("invalidId");
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(unsavedChallenge);
    UpdateChallengeResponse response = new UpdateChallengeResponse();
    response.setError(new Error(UpdateChallengeResponse.CHALLENGE_NOT_FOUND_ERROR));

    updateChallenge(user, "invalidId", requestBody)
        .andExpect(status().isBadRequest())
        .andExpect(content().json(JsonHandler.toJson(response)));
  }

  @Test
  public void updateChallenge_extraSubChallenge_shouldFail() throws Exception {
    Challenge challenge = createNewChallenge(user);
    List<SubChallenge> updatedSubChallenges = ImmutableList.of(
        challenge.getSubChallenges().get(0),
        challenge.getSubChallenges().get(0),
        challenge.getSubChallenges().get(1));
    updatedSubChallenges.get(2).setOriginalRepetitions(NEW_SUB_CHALLENGE_2_LEFT_REPETITIONS);
    challenge.setSubChallenges(updatedSubChallenges);
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(challenge);
    UpdateChallengeResponse response = new UpdateChallengeResponse();
    response.setError(new Error(UpdateChallengeResponse.MISSING_OR_DUPLICATED_SUB_CHALLENGE_ERROR));

    updateChallenge(user, challenge.getId(), requestBody)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().json(JsonHandler.toJson(response)));

    ChallengeProgress updatedChallenge = getChallengeProgressFromApi(challenge);
    assertThat(updatedChallenge.getSubChallenges().get(0).getLeftRepetitions(), is(
        OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS));
    assertThat(updatedChallenge.getSubChallenges().get(1).getLeftRepetitions(), is(
        OLD_SUB_CHALLENGE_2_LEFT_REPETITIONS));
  }

  @Test
  public void updateChallenge_missingSubChallenge_shouldFail() throws Exception {
    Challenge challenge = createNewChallenge(user);
    List<SubChallenge> updatedSubChallenges = ImmutableList.of(
        challenge.getSubChallenges().get(0));
    updatedSubChallenges.get(0).setOriginalRepetitions(NEW_SUB_CHALLENGE_1_LEFT_REPETITIONS);
    challenge.setSubChallenges(updatedSubChallenges);
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(challenge);
    UpdateChallengeResponse response = new UpdateChallengeResponse();
    response.setError(new Error(UpdateChallengeResponse.MISSING_OR_DUPLICATED_SUB_CHALLENGE_ERROR));

    updateChallenge(user, challenge.getId(), requestBody)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().json(JsonHandler.toJson(response)));

    ChallengeProgress updatedChallenge = getChallengeProgressFromApi(challenge);
    assertThat(updatedChallenge.getSubChallenges().get(0).getLeftRepetitions(), is(
        OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS));
    assertThat(updatedChallenge.getSubChallenges().get(1).getLeftRepetitions(), is(
        OLD_SUB_CHALLENGE_2_LEFT_REPETITIONS));
  }

  @Test
  public void updateChallenge_duplicatedSubChallenge_shouldFail() throws Exception {
    Challenge challenge = createNewChallenge(user);
    List<SubChallenge> updatedSubChallenges = ImmutableList.of(
        challenge.getSubChallenges().get(0),
        challenge.getSubChallenges().get(0));
    updatedSubChallenges.get(0).setOriginalRepetitions(NEW_SUB_CHALLENGE_1_LEFT_REPETITIONS);
    updatedSubChallenges.get(1).setOriginalRepetitions(NEW_SUB_CHALLENGE_1_LEFT_REPETITIONS);
    challenge.setSubChallenges(updatedSubChallenges);
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(challenge);
    UpdateChallengeResponse response = new UpdateChallengeResponse();
    response.setError(new Error(UpdateChallengeResponse.MISSING_OR_DUPLICATED_SUB_CHALLENGE_ERROR));

    updateChallenge(user, challenge.getId(), requestBody)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().json(JsonHandler.toJson(response)));

    ChallengeProgress updatedChallenge = getChallengeProgressFromApi(challenge);
    assertThat(updatedChallenge.getSubChallenges().get(0).getLeftRepetitions(), is(
        OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS));
    assertThat(updatedChallenge.getSubChallenges().get(1).getLeftRepetitions(), is(
        OLD_SUB_CHALLENGE_2_LEFT_REPETITIONS));
  }

  protected abstract Challenge createNewChallenge(User user) throws Exception;

  protected abstract ResultActions updateChallenge(User user, String challengeId,
      UpdateChallengeRequest requestBody) throws Exception;

  protected abstract ChallengeProgress getChallengeProgressFromApi(Challenge challenge)
      throws Exception;
}
