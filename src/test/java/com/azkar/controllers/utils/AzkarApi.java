package com.azkar.controllers.utils;

import com.azkar.entities.Challenge;
import com.azkar.entities.User;
import com.azkar.payload.challengecontroller.requests.AddChallengeRequest;
import com.azkar.payload.challengecontroller.requests.AddPersonalChallengeRequest;
import com.azkar.payload.challengecontroller.requests.UpdateChallengeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.ResultActions;

@Service
public class AzkarApi {

  @Autowired
  HttpClient httpClient;

  public ResultActions getProfile(User user) throws Exception {
    return httpClient.performGetRequest(user, "/users/me");
  }

  public ResultActions getProfileWithoutAuthentication() throws Exception {
    return httpClient.performGetRequest(/*user=*/ (User) null, "/users/me");
  }

  public ResultActions getChallenge(User user, String challengeId) throws Exception {
    return httpClient.performGetRequest(user, String.format("/challenges/%s", challengeId));
  }

  public ResultActions createChallenge(User user, Challenge challenge) throws Exception {
    return httpClient.performPostRequest(user, "/challenges",
        JsonHandler.toJson(new AddChallengeRequest(challenge)));
  }

  public ResultActions createPersonalChallenge(User user, AddPersonalChallengeRequest request)
      throws Exception {
    return
        httpClient.performPostRequest(user, "/challenges/personal", JsonHandler.toJson(request));
  }

  public ResultActions getOngoingChallenges(User user, String groupId) throws Exception {
    return httpClient
        .performGetRequest(user, String.format("/challenges/groups/%s/ongoing/", groupId));
  }

  public ResultActions getProposedChallenges(User user, String groupId) throws Exception {
    return httpClient
        .performGetRequest(user, String.format("/challenges/groups/%s/proposed/", groupId));
  }

  public ResultActions getOngoingChallenges(User user) throws Exception {
    return httpClient
        .performGetRequest(user, "/challenges/ongoing/");
  }

  public ResultActions getProposedChallenges(User user) throws Exception {
    return httpClient
        .performGetRequest(user, "/challenges/proposed/");
  }

  public ResultActions updateChallenge(User user, String challengeId,
      UpdateChallengeRequest request) throws Exception {
    return httpClient.performPutRequest(user, String.format("/challenges/%s", challengeId),
        JsonHandler.toJson(request));
  }
}
