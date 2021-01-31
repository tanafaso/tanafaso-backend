package com.azkar.controllers.challengecontroller;

import com.azkar.entities.Challenge;
import com.azkar.entities.User;
import com.azkar.entities.User.ChallengeProgress;
import com.azkar.payload.challengecontroller.requests.UpdateChallengeRequest;
import com.azkar.payload.challengecontroller.responses.GetChallengesResponse;
import org.junit.Before;
import org.springframework.test.web.servlet.ResultActions;

public class UpdatePersonalChallengeTest extends UpdateChallengeTestBase {

  @Before
  public void setup() {
    super.setup();
  }

  // TODO(#129): This is a hack to return a challenge using challenge index in the personal
  //  challenges array just to make tests pass for now. This should be changed once
  //  getPersonalChallenge is implemented.
  private ChallengeProgress getChallengeProgressFromApi(Challenge challenge, int challengeIdx)
      throws Exception {
    return getResponse(azkarApi.getPersonalChallenges(user),
        GetChallengesResponse.class).getData().get(challengeIdx).getChallengeProgress();
  }

  @Override
  public ResultActions updateChallenge(User user, String challengeId,
      UpdateChallengeRequest requestBody) throws Exception {
    return azkarApi.updatePersonalChallenge(user, challengeId, requestBody);
  }

  @Override
  protected ChallengeProgress getChallengeProgressFromApi(Challenge challenge)
      throws Exception {
    return getChallengeProgressFromApi(challenge, 0);
  }

  @Override
  protected Challenge createNewChallenge(User user) throws Exception {
    return createPersonalChallenge(user);
  }
}
