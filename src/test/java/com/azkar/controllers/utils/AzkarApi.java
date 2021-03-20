package com.azkar.controllers.utils;

import com.azkar.entities.Challenge;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.payload.challengecontroller.requests.AddChallengeRequest;
import com.azkar.payload.challengecontroller.requests.AddPersonalChallengeRequest;
import com.azkar.payload.challengecontroller.requests.UpdateChallengeRequest;
import com.azkar.payload.challengecontroller.responses.AddChallengeResponse;
import com.azkar.payload.challengecontroller.responses.GetChallengeResponse;
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

  public Challenge getChallengeAndReturn(User user, String challengeId) throws Exception {
    MvcResult result = httpClient.performGetRequest(user, String.format("/challenges/%s",
        challengeId)).andReturn();
    GetChallengeResponse response =
        JsonHandler.fromJson(result.getResponse().getContentAsString(), GetChallengeResponse.class);
    return response.getData();
  }

  public Challenge createChallengeAndReturn(User user, Challenge challenge) throws Exception {
    MvcResult result = createChallenge(user, challenge).andReturn();
    AddChallengeResponse response = JsonHandler.fromJson(result.getResponse().getContentAsString(),
        AddChallengeResponse.class);
    return response.getData();
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

  public ResultActions getAllChallengesInGroup(User user, String groupId) throws Exception {
    return httpClient
        .performGetRequest(user, String.format("/challenges/groups/%s/", groupId));
  }

  public ResultActions getAllChallenges(User user) throws Exception {
    return httpClient
        .performGetRequest(user, "/challenges/");
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

  public ResultActions addGroup(User user, String name) throws Exception {
    return httpClient.performPostRequest(user, "/groups",
        JsonHandler.toJson(AddGroupRequest.builder().name(name).build()));
  }

  public ResultActions addGroup(User user, AddGroupRequest body) throws Exception {
    return httpClient.performPostRequest(user, "/groups", JsonHandler.toJson(body));
  }

  public ResultActions getGroup(User user, String groupId) throws Exception {
    return httpClient.performGetRequest(user, String.format("/groups/%s", groupId));
  }

  public ResultActions getGroups(User user) throws Exception {
    return httpClient.performGetRequest(user, "/groups");
  }

  public ResultActions getGroupLeaderboard(User user, String groupId) throws Exception {
    return httpClient.performGetRequest(user, String.format("/groups/%s/leaderboard", groupId));
  }

  public ResultActions inviteUserToGroup(User invitingUser, User invitedUser, String groupId)
      throws Exception {
    return httpClient.performPutRequest(invitingUser, String.format("/groups/%s/invite/%s", groupId,
        invitedUser.getId()),
        /*body=*/ null);
  }

  public ResultActions acceptInvitationToGroup(User user, String groupId)
      throws Exception {
    return httpClient.performPutRequest(user, String.format("/groups/%s/accept/", groupId),
        /*body=*/ null);
  }

  public ResultActions addUserToGroup(User user, User invitingUser, String groupId)
      throws Exception {
    inviteUserToGroup(invitingUser, user, groupId);
    return acceptInvitationToGroup(user, groupId);
  }
}
