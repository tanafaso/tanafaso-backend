package com.azkar.controllers.challengecontroller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.TestBase;
import com.azkar.entities.Challenge;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.entities.User.UserChallengeStatus;
import com.azkar.factories.entities.ChallengeFactory;
import com.azkar.factories.entities.GroupFactory;
import com.azkar.factories.entities.UserFactory;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.challengecontroller.requests.AddChallengeRequest;
import com.azkar.payload.challengecontroller.responses.GetChallengeResponse;
import com.azkar.payload.challengecontroller.responses.GetChallengesResponse.UserReturnedChallenge;
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
    UserReturnedChallenge queriedChallenge = createOngoingUserChallenge(user, group);
    UserReturnedChallenge anotherChallenge = createOngoingUserChallenge(user, group);
    GetChallengeResponse response = new GetChallengeResponse();
    response.setData(queriedChallenge);

    String path = getChallengePath(queriedChallenge.getChallengeInfo());
    performGetRequest(user, path)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(response)));
  }

  private String getChallengePath(Challenge challenge) {
    return String.format("/challenges/%s", challenge.getId());
  }

  private UserReturnedChallenge createOngoingUserChallenge(User user, Group group)
      throws Exception {
    Challenge challenge = ChallengeFactory.getNewChallenge(group.getId());
    addChallenge(user, challenge);
    challenge.setOngoing(true);
    UserChallengeStatus userChallengeStatus = new UserChallengeStatus(challenge.getId(),
        /* isAccepted= */true,
        challenge.isOngoing(),
        challenge.getSubChallenges());
    return new UserReturnedChallenge(challenge, userChallengeStatus);
  }

  @Test
  public void getChallenge_invalidChallengeId_shouldFail() throws Exception {
    GetChallengeResponse notFoundResponse = getGetChallengeNotFoundResponse();

    performGetRequest(user, "/challenges/invalidId")
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(notFoundResponse)));
  }

  private GetChallengeResponse getGetChallengeNotFoundResponse() {
    GetChallengeResponse expectedResponse = new GetChallengeResponse();
    expectedResponse.setError(new Error(GetChallengeResponse.CHALLENGE_NOT_FOUND_ERROR));
    return expectedResponse;
  }

  @Test
  public void getChallenge_userDoesNotHaveChallenge_shouldFail() throws Exception {
    Challenge challenge = ChallengeFactory.getNewChallenge(group.getId());
    addChallenge(user, challenge);
    User nonGroupMember = UserFactory.getNewUser();
    addNewUser(nonGroupMember);
    GetChallengeResponse notFoundResponse = getGetChallengeNotFoundResponse();

    String path = getChallengePath(challenge);
    performGetRequest(nonGroupMember, path)
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(notFoundResponse)));
  }

  private void addChallenge(User user, Challenge challenge) throws Exception {
    performPostRequest(user, "/challenges", mapToJson(new AddChallengeRequest(challenge)))
        .andExpect(status().isOk());
  }
}
