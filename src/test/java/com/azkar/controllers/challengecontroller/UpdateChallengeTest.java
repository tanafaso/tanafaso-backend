package com.azkar.controllers.challengecontroller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.TestBase;
import com.azkar.controllers.utils.JsonHandler;
import com.azkar.entities.Challenge;
import com.azkar.entities.Challenge.SubChallenges;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.entities.User.UserChallengeStatus;
import com.azkar.factories.entities.ChallengeFactory;
import com.azkar.factories.entities.GroupFactory;
import com.azkar.factories.entities.UserFactory;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.challengecontroller.requests.UpdateChallengeRequest;
import com.azkar.payload.challengecontroller.requests.UpdateChallengeRequest.ModifiedSubChallenge;
import com.azkar.payload.challengecontroller.responses.GetChallengeResponse;
import com.azkar.payload.challengecontroller.responses.UpdateChallengeResponse;
import com.azkar.repos.GroupRepo;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

public class UpdateChallengeTest extends TestBase {

  public static final int OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS = ChallengeFactory.SUB_CHALLENGE_1
      .getOriginalRepetitions();
  public static final int OLD_SUB_CHALLENGE_2_LEFT_REPETITIONS = ChallengeFactory.SUB_CHALLENGE_2
      .getOriginalRepetitions();
  public static final int NEW_SUB_CHALLENGE_1_LEFT_REPETITIONS =
      OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS - 1;
  public static final int NEW_SUB_CHALLENGE_2_LEFT_REPETITIONS =
      OLD_SUB_CHALLENGE_2_LEFT_REPETITIONS - 2;
  private static final ModifiedSubChallenge MODIFIED_SUB_CHALLENGE_1 =
      createModifiedSubChallenge(ChallengeFactory.SUB_CHALLENGE_1,
          NEW_SUB_CHALLENGE_1_LEFT_REPETITIONS);
  private static final ModifiedSubChallenge MODIFIED_SUB_CHALLENGE_2 =
      createModifiedSubChallenge(ChallengeFactory.SUB_CHALLENGE_2,
          NEW_SUB_CHALLENGE_2_LEFT_REPETITIONS);
  @Autowired
  GroupRepo groupRepo;
  private User user;
  private Group group;

  private static UpdateChallengeRequest createUpdateChallengeRequest(
      ModifiedSubChallenge... modifiedSubChallenges) {
    List<ModifiedSubChallenge> allModifiedSubChallenges = ImmutableList
        .copyOf(modifiedSubChallenges);
    return UpdateChallengeRequest.builder()
        .allModifiedSubChallenges(allModifiedSubChallenges).build();
  }

  private static ModifiedSubChallenge createModifiedSubChallenge(SubChallenges subChallenges,
      int newLeftRepetition) {
    return ModifiedSubChallenge.builder().zekrId(subChallenges.getZekrId())
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
  public void testDoZekr_updateOneSubChallenge_shouldSucceed() throws Exception {
    Challenge challenge = createNewChallenge(user, group.getId());
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(
        MODIFIED_SUB_CHALLENGE_2);

    azkarApi.updateChallenge(user, challenge.getId(), requestBody)
        .andExpect(status().isOk());

    UserChallengeStatus updatedChallenge = getUserChallengeStatusFromApi(challenge);
    assertThat(updatedChallenge.getSubChallenges().get(0).getLeftRepetitions(), is(
        OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS));
    assertThat(updatedChallenge.getSubChallenges().get(1).getLeftRepetitions(), is(
        NEW_SUB_CHALLENGE_2_LEFT_REPETITIONS));

  }

  private UserChallengeStatus getUserChallengeStatusFromApi(Challenge challenge) throws Exception {
    ResultActions resultActions = azkarApi.getChallenge(user, challenge.getId())
        .andExpect(status().isOk());
    return getResponse(resultActions, GetChallengeResponse.class)
        .getData()
        .getUserChallengeStatus();
  }

  @Test
  public void testDoZekr_updateMultipleSubChallenges_shouldSucceed() throws Exception {
    Challenge challenge = createNewChallenge(user, group.getId());
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(
        MODIFIED_SUB_CHALLENGE_1, MODIFIED_SUB_CHALLENGE_2);

    azkarApi.updateChallenge(user, challenge.getId(), requestBody)
        .andExpect(status().isOk());

    UserChallengeStatus updatedChallenge = getUserChallengeStatusFromApi(challenge);
    assertThat(updatedChallenge.getSubChallenges().get(0).getLeftRepetitions(),
        is(NEW_SUB_CHALLENGE_1_LEFT_REPETITIONS));
    assertThat(updatedChallenge.getSubChallenges().get(1).getLeftRepetitions(),
        is(NEW_SUB_CHALLENGE_2_LEFT_REPETITIONS));
  }

  @Test
  public void testDoZekr_NegativeLeftRepetitions_shouldUpdateWithZero() throws Exception {
    Challenge challenge = createNewChallenge(user, group.getId());
    ModifiedSubChallenge modifiedSubChallenge1WithNegativeRepetitions = createModifiedSubChallenge(
        ChallengeFactory.SUB_CHALLENGE_1, /*newLeftRepetition*/ -1);
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(
        modifiedSubChallenge1WithNegativeRepetitions);

    azkarApi.updateChallenge(user, challenge.getId(), requestBody)
        .andExpect(status().isOk());

    UserChallengeStatus updatedChallenge = getUserChallengeStatusFromApi(challenge);
    assertThat(updatedChallenge.getSubChallenges().get(0).getLeftRepetitions(),
        is(0));
    assertThat(updatedChallenge.getSubChallenges().get(1).getLeftRepetitions(),
        is(OLD_SUB_CHALLENGE_2_LEFT_REPETITIONS));

  }

  @Test
  public void testDoZekr_IncrementLeftRepetitions_shouldFail() throws Exception {
    Challenge challenge = createNewChallenge(user, group.getId());
    ModifiedSubChallenge modifiedSubChallengeWithIncreasingRepetitions =
        createModifiedSubChallenge(
            ChallengeFactory.SUB_CHALLENGE_1,
            OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS + 1);
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(
        modifiedSubChallengeWithIncreasingRepetitions);
    UpdateChallengeResponse expectedResponse = new UpdateChallengeResponse();
    expectedResponse
        .setError(new Error(UpdateChallengeResponse.INCREMENTING_LEFT_REPETITIONS_ERROR));

    azkarApi.updateChallenge(user, challenge.getId(), requestBody)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    UserChallengeStatus updatedChallenge = getUserChallengeStatusFromApi(challenge);
    assertThat(updatedChallenge.getSubChallenges().get(0).getLeftRepetitions(),
        is(OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS));
    assertThat(updatedChallenge.getSubChallenges().get(1).getLeftRepetitions(),
        is(OLD_SUB_CHALLENGE_2_LEFT_REPETITIONS));

  }

  @Test
  public void testDoZekr_invalidZekrId_shouldFail() throws Exception {
    String invalidZekrId = "777";
    Challenge challenge = createNewChallenge(user, group.getId());
    ModifiedSubChallenge invalidModifiedSubChallenge = ModifiedSubChallenge.builder()
        .zekrId(invalidZekrId).newLeftRepetitions(1).build();
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(
        MODIFIED_SUB_CHALLENGE_1, invalidModifiedSubChallenge);
    UpdateChallengeResponse expectedResponse = new UpdateChallengeResponse();
    expectedResponse
        .setError(new Error(UpdateChallengeResponse.NON_EXISTENT_SUB_CHALLENGE_ERROR));

    azkarApi.updateChallenge(user, challenge.getId(), requestBody)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    UserChallengeStatus updatedChallenge = getUserChallengeStatusFromApi(challenge);
    assertThat(updatedChallenge.getSubChallenges().get(0).getLeftRepetitions(),
        is(OLD_SUB_CHALLENGE_1_LEFT_REPETITIONS));
    assertThat(updatedChallenge.getSubChallenges().get(1).getLeftRepetitions(),
        is(OLD_SUB_CHALLENGE_2_LEFT_REPETITIONS));

  }

  @Test
  public void testDoZekr_invalidChallengeId() throws Exception {
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(MODIFIED_SUB_CHALLENGE_1);
    azkarApi.updateChallenge(user, "invalidId", requestBody)
        .andExpect(status().isBadRequest());
  }
}
