package com.azkar.controllers.challengecontroller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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
import com.azkar.payload.challengecontroller.requests.AddMeaningChallengeRequest;
import com.azkar.payload.challengecontroller.responses.AddMeaningChallengeResponse;
import com.azkar.payload.challengecontroller.responses.DeleteChallengeResponse;
import com.azkar.payload.challengecontroller.responses.GetChallengeResponse;
import com.azkar.payload.usercontroller.responses.GetFriendsResponse;
import com.azkar.repos.GroupRepo;
import com.azkar.repos.UserRepo;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class MeaningChallengeTest extends TestBase {

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
  public void addMeaningChallenge_normalScenario_shouldSucceed() throws Exception {
    int meaningChallengesCountBefore = user.getMeaningChallenges().size();

    MvcResult mvcResult = httpClient.performPostRequest(user, "/challenges/meaning",
        JsonHandler.toJson(new AddMeaningChallengeRequest()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    AddMeaningChallengeResponse response = JsonHandler
        .fromJson(mvcResult.getResponse().getContentAsString(), AddMeaningChallengeResponse.class);

    User updatedUser = userRepo.findById(user.ge)
    MeaningChallenge resultChallenge = response.getData()
    assertThat(response.getData().getMeanings().size(), is(3));
    assertThat(response.getData().getWords().size(), is(3));
    assertThat(response.getData().getWords().get(0), not());
  }

  @Test
  public void deleteChallenge_normalScenario_shouldSucceed() throws Exception {
    AzkarChallenge queriedChallenge = createMeaningGroupChallenge(user, group);
    AzkarChallenge anotherChallenge = createMeaningGroupChallenge(user, group);
    DeleteChallengeResponse response = new DeleteChallengeResponse();
    response.setData(queriedChallenge);
    List<AzkarChallenge> userChallenges = userRepo.findById(user.getId()).get().getAzkarChallenges();
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
  public void getOriginalChallenge_normalScenario_shouldSucceed() throws Exception {
    AzkarChallenge queriedChallenge = createMeaningGroupChallenge(user, group);
    // Create irrelevant challenge
    createMeaningGroupChallenge(user, group);

    // Change the user's copy of the challenge
    User updatedUser1 = userRepo.findById(user.getId()).get();
    updatedUser1.getUserChallenges().stream().forEach(
        userChallenge -> userChallenge.getSubChallenges().stream().forEach(
            subChallenge -> subChallenge.setRepetitions(subChallenge.getRepetitions() + 1)
        )
    );
    userRepo.save(updatedUser1);

    GetChallengeResponse response = new GetChallengeResponse();
    response.setData(queriedChallenge);

    azkarApi.getOriginalChallenge(user, queriedChallenge.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(response)));
  }

  @Test
  public void getOriginalChallenge_invalidChallengeId_shouldFail() throws Exception {
    GetChallengeResponse notFoundResponse = getGetChallengeNotFoundResponse();

    azkarApi.getOriginalChallenge(user, "invalidChallengeId")
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(notFoundResponse)));
  }

  @Test
  public void getOriginalChallenge_userDoesNotHaveChallenge_shouldFail() throws Exception {
    AzkarChallenge challenge = ChallengeFactory.getNewChallenge(group.getId());
    azkarApi.addAzkarChallenge(user, challenge).andExpect(status().isOk());
    User nonGroupMember = UserFactory.getNewUser();
    addNewUser(nonGroupMember);
    GetChallengeResponse notFoundResponse = getGetChallengeNotFoundResponse();

    azkarApi.getOriginalChallenge(nonGroupMember, challenge.getId())
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(notFoundResponse)));
  }

  @Test
  public void getChallenge_invalidChallengeId_shouldFail() throws Exception {
    GetChallengeResponse notFoundResponse = getGetChallengeNotFoundResponse();

    azkarApi.getChallenge(user, "invalidChallengeId")
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(notFoundResponse)));
  }

  @Test
  public void getChallenge_normalScenario_shouldSucceed() throws Exception {
    AzkarChallenge queriedChallenge = createMeaningGroupChallenge(user, group);
    AzkarChallenge anotherChallenge = createMeaningGroupChallenge(user, group);
    GetChallengeResponse response = new GetChallengeResponse();
    response.setData(queriedChallenge);

    azkarApi.getChallenge(user, queriedChallenge.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(response)));
  }

  @Test
  public void getChallenge_userDoesNotHaveChallenge_shouldFail() throws Exception {
    AzkarChallenge challenge = ChallengeFactory.getNewChallenge(group.getId());
    azkarApi.addAzkarChallenge(user, challenge).andExpect(status().isOk());
    User nonGroupMember = UserFactory.getNewUser();
    addNewUser(nonGroupMember);
    GetChallengeResponse notFoundResponse = getGetChallengeNotFoundResponse();

    azkarApi.getChallenge(nonGroupMember, challenge.getId())
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(notFoundResponse)));
  }

  private GetChallengeResponse getGetChallengeNotFoundResponse() {
    GetChallengeResponse expectedResponse = new GetChallengeResponse();
    expectedResponse.setStatus(new Status(Status.CHALLENGE_NOT_FOUND_ERROR));
    return expectedResponse;
  }
}
