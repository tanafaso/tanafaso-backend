package com.azkar.controllers.challengecontroller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.entities.Challenge;
import com.azkar.entities.Challenge.SubChallenge;
import com.azkar.entities.User;
import com.azkar.payload.challengecontroller.requests.UpdateChallengeRequest;
import com.azkar.payload.challengecontroller.responses.GetChallengeResponse;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.ResultActions;

public class UpdateGroupChallengeTest extends UpdateChallengeTestBase {

  @Before
  public void setup() throws Exception {
    super.setup();
  }

  @Test
  public void updateChallenge_finishChallenge_shouldUpdateScore() throws Exception {
    Challenge challenge = createNewChallenge(user);
    for(SubChallenge subChallenge: challenge.getSubChallenges()) {
      subChallenge.setRepetitions(0);
    }
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(challenge);

    updateChallenge(user, challenge.getId(), requestBody)
        .andExpect(status().isOk());

    Challenge updatedChallenge = getChallengeProgressFromApi(challenge);
    assertThat(updatedChallenge.getSubChallenges().get(0).getRepetitions(), is(
        0));
    assertThat(updatedChallenge.getSubChallenges().get(1).getRepetitions(), is(
        0));
    User userFromDb = userRepo.findById(user.getId()).get();
    assertThat(userFromDb.getUserGroups().get(0).getTotalScore(), is(1));
  }

  @Override
  protected ResultActions updateChallenge(User user, String challengeId,
      UpdateChallengeRequest requestBody) throws Exception {
    return azkarApi.updateChallenge(user, challengeId, requestBody);
  }

  @Override
  protected Challenge getChallengeProgressFromApi(Challenge challenge)
      throws Exception {
    ResultActions resultActions = azkarApi.getChallenge(user, challenge.getId())
        .andExpect(status().isOk());
    return getResponse(resultActions, GetChallengeResponse.class).getData();
  }

  @Override
  protected Challenge createNewChallenge(User user) throws Exception {
    return createGroupChallenge(user, group.getId());
  }
}
