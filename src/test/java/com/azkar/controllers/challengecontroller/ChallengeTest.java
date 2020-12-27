package com.azkar.controllers.challengecontroller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.TestBase;
import com.azkar.controllers.utils.JsonHandler;
import com.azkar.entities.Challenge;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.entities.User.UserChallengeStatus;
import com.azkar.factories.entities.ChallengeFactory;
import com.azkar.factories.entities.GroupFactory;
import com.azkar.factories.entities.UserFactory;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.challengecontroller.responses.GetChallengeResponse;
import com.azkar.payload.challengecontroller.responses.GetChallengesResponse.UserChallenge;
import com.azkar.repos.GroupRepo;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

public class ChallengeTest extends TestBase {

  private User user;
  private Group group;

  @Autowired
  private GroupRepo groupRepo;

  @Before
  public void setUp() {
    user = UserFactory.getNewUser();
    addNewUser(user);
    group = GroupFactory.getNewGroup(user.getId());
    groupRepo.save(group);
  }

  @Test
  public void getChallenge_normalScenario_shouldSucceed() throws Exception {
    UserChallenge queriedChallenge = createOngoingUserChallenge(user, group);
    UserChallenge anotherChallenge = createOngoingUserChallenge(user, group);
    GetChallengeResponse response = new GetChallengeResponse();
    response.setData(queriedChallenge);

    azkarApi.getChallenge(user, queriedChallenge.getChallengeInfo().getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(response)));
  }

  private UserChallenge createOngoingUserChallenge(User user, Group group)
      throws Exception {
    Challenge challenge = ChallengeFactory.getNewChallenge(group.getId());
    azkarApi.createChallenge(user, challenge).andExpect(status().isOk());
    challenge.setOngoing(true);
    UserChallengeStatus userChallengeStatus = new UserChallengeStatus(challenge.getId(),
        /* isAccepted= */true,
        challenge.isOngoing(),
        group.getId(),
        challenge.getSubChallenges());
    return new UserChallenge(challenge, userChallengeStatus);
  }

  @Test
  public void getChallenge_invalidChallengeId_shouldFail() throws Exception {
    GetChallengeResponse notFoundResponse = getGetChallengeNotFoundResponse();

    azkarApi.getChallenge(user, "invalidChallengeId")
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(notFoundResponse)));
  }

  private GetChallengeResponse getGetChallengeNotFoundResponse() {
    GetChallengeResponse expectedResponse = new GetChallengeResponse();
    expectedResponse.setError(new Error(GetChallengeResponse.CHALLENGE_NOT_FOUND_ERROR));
    return expectedResponse;
  }

  @Test
  public void getChallenge_userDoesNotHaveChallenge_shouldFail() throws Exception {
    Challenge challenge = ChallengeFactory.getNewChallenge(group.getId());
    azkarApi.createChallenge(user, challenge).andExpect(status().isOk());
    User nonGroupMember = UserFactory.getNewUser();
    addNewUser(nonGroupMember);
    GetChallengeResponse notFoundResponse = getGetChallengeNotFoundResponse();

    azkarApi.getChallenge(nonGroupMember, challenge.getId())
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(notFoundResponse)));
  }
}
