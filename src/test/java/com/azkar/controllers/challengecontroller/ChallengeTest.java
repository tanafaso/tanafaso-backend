package com.azkar.controllers.challengecontroller;

import static com.azkar.factories.entities.ChallengeFactory.EXPIRY_DATE_OFFSET;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.TestBase;
import com.azkar.controllers.utils.JsonHandler;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.entities.challenges.AzkarChallenge;
import com.azkar.entities.challenges.MeaningChallenge;
import com.azkar.factories.entities.ChallengeFactory;
import com.azkar.factories.entities.GroupFactory;
import com.azkar.factories.entities.UserFactory;
import com.azkar.payload.ResponseBase.Status;
import com.azkar.payload.challengecontroller.requests.AddAzkarChallengeRequest;
import com.azkar.payload.challengecontroller.requests.AddMeaningChallengeRequest;
import com.azkar.payload.challengecontroller.responses.AddAzkarChallengeResponse;
import com.azkar.payload.challengecontroller.responses.DeleteChallengeResponse;
import com.azkar.payload.challengecontroller.responses.GetChallengesV2Response;
import com.azkar.repos.GroupRepo;
import com.azkar.repos.UserRepo;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class ChallengeTest extends TestBase {

  @Autowired
  UserRepo userRepo;
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
  public void deleteChallenge_normalScenario_shouldSucceed() throws Exception {
    AzkarChallenge queriedChallenge = createGroupChallenge(user, group);
    AzkarChallenge anotherChallenge = createGroupChallenge(user, group);
    DeleteChallengeResponse response = new DeleteChallengeResponse();
    response.setData(queriedChallenge);
    List<AzkarChallenge> userChallenges =
        userRepo.findById(user.getId()).get().getAzkarChallenges();
    assertThat(userChallenges.size(), is(2));

    azkarApi.deleteChallenge(user, queriedChallenge.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(response)));

    userChallenges = userRepo.findById(user.getId()).get().getAzkarChallenges();
    assertThat(userChallenges.size(), is(1));
    assertThat(userChallenges.get(0).getId(), equalTo(anotherChallenge.getId()));
  }


  @Test
  public void deleteChallenge_invalidChallengeId_shouldFail() throws Exception {
    DeleteChallengeResponse notFoundResponse = new DeleteChallengeResponse();
    notFoundResponse.setStatus(new Status(Status.CHALLENGE_NOT_FOUND_ERROR));

    azkarApi.deleteChallenge(user, "invalidChallengeId")
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(notFoundResponse)));
  }

  @Test
  public void deleteChallenge_userDoesNotHaveChallenge_shouldFail() throws Exception {
    AzkarChallenge challenge = ChallengeFactory.getNewChallenge(group.getId());
    azkarApi.addAzkarChallenge(user, challenge).andExpect(status().isOk());
    User nonGroupMember = UserFactory.getNewUser();
    addNewUser(nonGroupMember);
    DeleteChallengeResponse notFoundResponse = new DeleteChallengeResponse();
    notFoundResponse.setStatus(new Status(Status.CHALLENGE_NOT_FOUND_ERROR));

    azkarApi.deleteChallenge(nonGroupMember, challenge.getId())
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(notFoundResponse)));
  }

  @Test
  public void getChallenges_normalScenario_shouldSucceed() throws Exception {
    User user1 = getNewRegisteredUser();
    User user2 = getNewRegisteredUser();
    User user3 = getNewRegisteredUser();

    azkarApi.makeFriends(user1, user2);
    azkarApi.makeFriends(user1, user3);

    // Add one meaning challenge.
    AddMeaningChallengeRequest addMeaningChallengeRequest = AddMeaningChallengeRequest.builder()
        .friendsIds(ImmutableList.of(user2.getId()))
        .expiryDate(Instant.now().getEpochSecond() + EXPIRY_DATE_OFFSET)
        .build();
    MeaningChallenge meaningChallengeResponse =
        azkarApi.addMeaningChallengeAndReturn(user1, addMeaningChallengeRequest);

    // Add one azkar challenge.
    AzkarChallenge challenge = ChallengeFactory.getNewChallenge("groupId").toBuilder()
        .groupId(null)
        .build();
    AddAzkarChallengeRequest addAzkarChallengeRequest =
        AddAzkarChallengeRequest.AddFriendsChallengeRequestBuilder().
            friendsIds(ImmutableList.of(user2.getId(), user3.getId()))
            .challenge(challenge)
            .build();
    MvcResult result = azkarApi.addFriendsChallenge(user1, addAzkarChallengeRequest)
        .andExpect(status().isOk())
        .andReturn();
    AddAzkarChallengeResponse addAzkarChallengeResponse =
        JsonHandler.fromJson(result.getResponse().getContentAsString(),
            AddAzkarChallengeResponse.class);
    AzkarChallenge resultChallenge = addAzkarChallengeResponse.getData();

    MvcResult mvcResult = httpClient
        .performGetRequest(user1, "/challenges/v2")
        .andExpect(status().isOk())
        .andReturn();
    GetChallengesV2Response response = JsonHandler
        .fromJson(mvcResult.getResponse().getContentAsString(), GetChallengesV2Response.class);
    assertThat(response.getData().size(), is(2));

    assertThat(response.getData().get(0).getAzkarChallenge(), is(nullValue()));
    assertThat(response.getData().get(1).getMeaningChallenge(), is(nullValue()));

    assertThat(response.getData().get(0).getMeaningChallenge().getId(),
        is(meaningChallengeResponse.getId()));
    assertThat(response.getData().get(1).getAzkarChallenge().getId(),
        is(addAzkarChallengeResponse.getData().getId()));
  }

  private AzkarChallenge createGroupChallenge(User user, Group group)
      throws Exception {
    AzkarChallenge challenge = ChallengeFactory.getNewChallenge(group.getId());
    return azkarApi.addAzkarChallengeAndReturn(user, challenge);
  }
}
