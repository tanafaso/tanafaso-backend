package com.azkar.controllers.challengecontroller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.entities.Challenge;
import com.azkar.entities.Challenge.SubChallenge;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.entities.User.UserGroup;
import com.azkar.factories.entities.ChallengeFactory;
import com.azkar.factories.entities.UserFactory;
import com.azkar.payload.challengecontroller.requests.UpdateChallengeRequest;
import com.azkar.payload.challengecontroller.responses.GetChallengeResponse;
import com.azkar.repos.ChallengeRepo;
import com.google.common.collect.Iterators;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

public class UpdateGroupChallengeTest extends UpdateChallengeTestBase {

  @Autowired
  ChallengeRepo challengeRepo;

  @Before
  public void setup() throws Exception {
    super.setup();
  }

  @Test
  public void updateChallenge_finishChallenge_shouldUpdateScore() throws Exception {
    User user1 = UserFactory.getNewUser();
    addNewUser(user1);

    Group group1 = azkarApi.addGroupAndReturn(user1, "group1");
    Group group2 = azkarApi.addGroupAndReturn(user1, "group2");
    User user2InGroup1 = UserFactory.getNewUser();
    addNewUser(user2InGroup1);
    azkarApi.makeFriends(user1, user2InGroup1);
    azkarApi.addUserToGroup(/*invitingUser*/user1, /*user=*/user2InGroup1, group1.getId());

    Challenge challenge = createGroupChallenge(user1, group1.getId());
    for (SubChallenge subChallenge : challenge.getSubChallenges()) {
      subChallenge.setRepetitions(0);
    }
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(challenge);

    updateChallenge(user1, challenge.getId(), requestBody)
        .andExpect(status().isOk());

    Challenge updatedUser1Challenge = azkarApi.getChallengeAndReturn(user1, challenge.getId());
    Challenge updatedUser2Challenge =
        azkarApi.getChallengeAndReturn(user2InGroup1, challenge.getId());
    assertThat(updatedUser1Challenge.getSubChallenges().get(0).getRepetitions(), is(
        0));
    assertThat(updatedUser1Challenge.getSubChallenges().get(1).getRepetitions(), is(
        0));

    User updatedUser1 = userRepo.findById(user1.getId()).get();
    User updatedUser2 = userRepo.findById(user2InGroup1.getId()).get();

    assertThat(Iterators.getOnlyElement(updatedUser1Challenge.getUsersFinished().iterator()),
        equalTo(user1.getId()));
    assertThat(Iterators.getOnlyElement(updatedUser2Challenge.getUsersFinished().iterator()),
        equalTo(user1.getId()));
    Challenge updatedChallenge = challengeRepo.findById(challenge.getId()).get();
    assertThat(Iterators.getOnlyElement(updatedChallenge.getUsersFinished().iterator()),
        equalTo(user1.getId()));

    List<UserGroup> user1Groups = updatedUser1.getUserGroups();
    UserGroup userGroup1ForUser1 =
        user1Groups.stream().filter(userGroup -> userGroup.getGroupId().equals(group1.getId()))
            .findAny().get();
    UserGroup userGroup2ForUser1 =
        user1Groups.stream().filter(userGroup -> userGroup.getGroupId().equals(group2.getId()))
            .findAny().get();

    assertThat(userGroup1ForUser1.getTotalScore(), is(1));
    assertThat(userGroup2ForUser1.getTotalScore(), is(0));

    List<UserGroup> user2Groups = updatedUser2.getUserGroups();
    UserGroup userGroup1ForUser2 =
        user2Groups.stream().filter(userGroup -> userGroup.getGroupId().equals(group1.getId()))
            .findAny().get();
    assertThat(userGroup1ForUser2.getTotalScore(), is(0));
  }

  @Test
  public void updateChallenge_finishChallengeTwice_shouldUpdateScoreOnce() throws Exception {
    assertThat(userRepo.findById(user.getId()).get().getUserGroups().get(0).getTotalScore(), is(0));

    Challenge challenge = createGroupChallenge(user, group.getId());
    for (SubChallenge subChallenge : challenge.getSubChallenges()) {
      subChallenge.setRepetitions(0);
    }

    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(challenge);
    updateChallenge(user, challenge.getId(), requestBody)
        .andExpect(status().isOk());

    updateChallenge(user, challenge.getId(), requestBody)
        .andExpect(status().isOk());

    User updatedUser = userRepo.findById(user.getId()).get();
    assertThat(updatedUser.getUserGroups().size(), is(1));

    UserGroup userGroup = updatedUser.getUserGroups().get(0);
    assertThat(userGroup.getTotalScore(), is(1));
  }

  @Test
  public void updateChallenge_partiallyFinishedChallenge_shouldNotUpdateScore() throws Exception {
    assertThat(userRepo.findById(user.getId()).get().getUserGroups().get(0).getTotalScore(), is(0));

    Challenge challenge = ChallengeFactory.getNewChallenge(group.getId());
    assertThat(challenge.getSubChallenges().size(), not(0));
    challenge.getSubChallenges().get(0).setRepetitions(2);

    Challenge createdChallenge = createGroupChallenge(user, challenge);
    for (SubChallenge subChallenge : createdChallenge.getSubChallenges()) {
      subChallenge.setRepetitions(1);
    }

    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(createdChallenge);
    updateChallenge(user, createdChallenge.getId(), requestBody)
        .andExpect(status().isOk());

    User updatedUser = userRepo.findById(user.getId()).get();
    assertThat(updatedUser.getUserGroups().size(), is(1));

    UserGroup userGroup = updatedUser.getUserGroups().get(0);
    assertThat(userGroup.getTotalScore(), is(0));
  }

  @Override
  protected ResultActions updateChallenge(User user, String challengeId,
      UpdateChallengeRequest requestBody) throws Exception {
    return azkarApi.updateChallenge(user, challengeId, requestBody);
  }

  // Use azkarApi.getChallengeAndReturn instead.
  @Deprecated
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
