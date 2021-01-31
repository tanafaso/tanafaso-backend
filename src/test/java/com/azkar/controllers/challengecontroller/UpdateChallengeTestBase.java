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
import com.azkar.payload.challengecontroller.requests.UpdateChallengeRequest.ModifiedSubChallenge;
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

public abstract class UpdateChallengeTestBase extends TestBase {

  public static final int OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS = ChallengeFactory.SUB_CHALLENGE_1
      .getOriginalRepetitions();
  public static final int NEW_SUB_CHALLENGE_1_LEFT_REPETITIONS =
      OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS - 1;
  public static final ModifiedSubChallenge MODIFIED_SUB_CHALLENGE_1 =
      createModifiedSubChallenge(ChallengeFactory.SUB_CHALLENGE_1,
          NEW_SUB_CHALLENGE_1_LEFT_REPETITIONS);
  public static final int OLD_SUB_CHALLENGE_2_LEFT_REPETITIONS = ChallengeFactory.SUB_CHALLENGE_2
      .getOriginalRepetitions();
  public static final int NEW_SUB_CHALLENGE_2_LEFT_REPETITIONS =
      OLD_SUB_CHALLENGE_2_LEFT_REPETITIONS - 2;
  public static final ModifiedSubChallenge MODIFIED_SUB_CHALLENGE_2 =
      createModifiedSubChallenge(ChallengeFactory.SUB_CHALLENGE_2,
          NEW_SUB_CHALLENGE_2_LEFT_REPETITIONS);
  protected User user;
  @Autowired
  protected GroupRepo groupRepo;
  @Autowired
  protected UserRepo userRepo;
  protected Group group;

  static UpdateChallengeRequest createUpdateChallengeRequest(
      ModifiedSubChallenge... modifiedSubChallenges) {
    List<ModifiedSubChallenge> allModifiedSubChallenges = ImmutableList
        .copyOf(modifiedSubChallenges);
    return UpdateChallengeRequest.builder()
        .allModifiedSubChallenges(allModifiedSubChallenges).build();
  }

  static ModifiedSubChallenge createModifiedSubChallenge(SubChallenge subChallenge,
      int newLeftRepetition) {
    return ModifiedSubChallenge.builder().zekrId(subChallenge.getZekrId())
        .newLeftRepetitions(newLeftRepetition).build();
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
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(
        MODIFIED_SUB_CHALLENGE_2);

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
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(
        MODIFIED_SUB_CHALLENGE_1, MODIFIED_SUB_CHALLENGE_2);

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
    ModifiedSubChallenge modifiedSubChallenge1WithNegativeRepetitions = createModifiedSubChallenge(
        ChallengeFactory.SUB_CHALLENGE_1, /*newLeftRepetition*/ -1);
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(
        modifiedSubChallenge1WithNegativeRepetitions);

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
    ModifiedSubChallenge modifiedSubChallengeWithIncreasingRepetitions =
        createModifiedSubChallenge(
            ChallengeFactory.SUB_CHALLENGE_1,
            OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS + 1);
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(
        modifiedSubChallengeWithIncreasingRepetitions);
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
    ModifiedSubChallenge invalidModifiedSubChallenge = ModifiedSubChallenge.builder()
        .zekrId(invalidZekrId).newLeftRepetitions(1).build();
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(
        MODIFIED_SUB_CHALLENGE_1, invalidModifiedSubChallenge);
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
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(MODIFIED_SUB_CHALLENGE_1);
    UpdateChallengeResponse response = new UpdateChallengeResponse();
    response.setError(new Error(UpdateChallengeResponse.CHALLENGE_NOT_FOUND_ERROR));

    updateChallenge(user, "invalidId", requestBody)
        .andExpect(status().isBadRequest())
        .andExpect(content().json(JsonHandler.toJson(response)));
  }

  protected abstract Challenge createNewChallenge(User user) throws Exception;

  protected abstract ResultActions updateChallenge(User user, String challengeId,
      UpdateChallengeRequest requestBody) throws Exception;

  protected abstract ChallengeProgress getChallengeProgressFromApi(Challenge challenge)
      throws Exception;
}
