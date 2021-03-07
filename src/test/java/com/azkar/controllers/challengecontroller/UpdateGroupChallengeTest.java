package com.azkar.controllers.challengecontroller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.entities.Challenge;
import com.azkar.entities.User;
import com.azkar.payload.challengecontroller.requests.UpdateChallengeRequest;
import com.azkar.payload.challengecontroller.responses.GetChallengeResponse;
import org.junit.Before;
import org.springframework.test.web.servlet.ResultActions;

public class UpdateGroupChallengeTest extends UpdateChallengeTestBase {

  @Before
  public void setup() {
    super.setup();
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
