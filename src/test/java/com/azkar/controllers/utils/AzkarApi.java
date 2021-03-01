package com.azkar.controllers.utils;

import com.azkar.entities.Challenge;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.payload.challengecontroller.requests.AddChallengeRequest;
import com.azkar.payload.challengecontroller.requests.AddPersonalChallengeRequest;
import com.azkar.payload.challengecontroller.requests.UpdateChallengeRequest;
import com.azkar.payload.groupcontroller.requests.AddGroupRequest;
import com.azkar.payload.groupcontroller.responses.AddGroupResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.MvcResult;
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

  public ResultActions searchForUserByUsername(User user, String username) throws Exception {
    return httpClient.performGetRequest(user, String.format("/users/search?username=%s", username));
  }

  public ResultActions searchForUserByFacebookUserId(User user, String facebookUserId)
      throws Exception {
    return httpClient.performGetRequest(user, String.format("/users/search?facebook_user_id=%s",
        facebookUserId));
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

  public ResultActions getPersonalChallenges(User user) throws Exception {
    return httpClient.performGetRequest(user, "/challenges/personal");
  }

  public ResultActions updatePersonalChallenge(User user, String challengeId,
      UpdateChallengeRequest body)
      throws Exception {
    return httpClient.performPutRequest(user, String.format("/challenges/personal/%s", challengeId),
        JsonHandler.toJson(body));
  }

  public Group addGroupAndReturn(User user, String groupName) throws Exception {
    AddGroupRequest request = AddGroupRequest.builder().name(groupName).build();
    MvcResult result = addGroup(user, request).andReturn();
    AddGroupResponse response = JsonHandler.fromJson(result.getResponse().getContentAsString(),
        AddGroupResponse.class);
    return response.getData();
  }

  public ResultActions addGroup(User user, AddGroupRequest body) throws Exception {
    return httpClient.performPostRequest(user, "/groups", JsonHandler.toJson(body));
  }

  public ResultActions getGroups(User user) throws Exception {
    return httpClient.performGetRequest(user, "/groups");
  }
}
