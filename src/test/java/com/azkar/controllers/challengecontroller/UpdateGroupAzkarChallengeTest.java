package com.azkar.controllers.challengecontroller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.entities.Friendship.Friend;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.entities.User.UserGroup;
import com.azkar.entities.challenges.AzkarChallenge;
import com.azkar.entities.challenges.AzkarChallenge.SubChallenge;
import com.azkar.factories.entities.ChallengeFactory;
import com.azkar.factories.entities.UserFactory;
import com.azkar.payload.challengecontroller.requests.UpdateChallengeRequest;
import com.azkar.payload.challengecontroller.responses.GetChallengeResponse;
import com.azkar.repos.AzkarChallengeRepo;
import com.google.common.collect.Iterators;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

public class UpdateGroupAzkarChallengeTest extends UpdateAzkarChallengeTestBase {

  @Autowired
  AzkarChallengeRepo challengeRepo;

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

    AzkarChallenge challenge = createGroupChallenge(user1, group1.getId());
    for (SubChallenge subChallenge : challenge.getSubChallenges()) {
      subChallenge.setRepetitions(0);
    }
    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(challenge);

    updateChallenge(user1, challenge.getId(), requestBody)
        .andExpect(status().isOk());

    AzkarChallenge updatedUser1Challenge = azkarApi.getChallengeAndReturn(user1, challenge.getId());
    AzkarChallenge updatedUser2Challenge =
        azkarApi.getChallengeAndReturn(user2InGroup1, challenge.getId());
    assertThat(updatedUser1Challenge.getSubChallenges().get(0).getRepetitions(), is(
        0));
    assertThat(updatedUser1Challenge.getSubChallenges().get(1).getRepetitions(), is(
        0));

    assertThat(Iterators.getOnlyElement(updatedUser1Challenge.getUsersFinished().iterator()),
        equalTo(user1.getId()));
    assertThat(Iterators.getOnlyElement(updatedUser2Challenge.getUsersFinished().iterator()),
        equalTo(user1.getId()));
    AzkarChallenge updatedChallenge = challengeRepo.findById(challenge.getId()).get();
    assertThat(Iterators.getOnlyElement(updatedChallenge.getUsersFinished().iterator()),
        equalTo(user1.getId()));

    Friend user1Friend = azkarApi.getFriendsLeaderboardV2AndReturn(user1).stream()
        .filter(friend -> friend.getUserId().equals(user2InGroup1.getId())).findFirst().get();
    Friend user2Friend = azkarApi.getFriendsLeaderboardV2AndReturn(user2InGroup1).stream()
        .filter(friend -> friend.getUserId().equals(user1.getId())).findFirst().get();

    assertThat(user1Friend.getUserTotalScore(), is(1L));
    assertThat(user1Friend.getFriendTotalScore(), is(0L));

    assertThat(user2Friend.getUserTotalScore(), is(0L));
    assertThat(user2Friend.getFriendTotalScore(), is(1L));
  }

  @Test
  public void updateChallenge_partiallyFinishedChallenge_shouldNotUpdateScore() throws Exception {
    int userGroupsCountBefore = user.getUserGroups().size();
    assertThat(userRepo.findById(user.getId()).get().getUserGroups().get(0).getTotalScore(), is(0));

    AzkarChallenge challenge = ChallengeFactory.getNewChallenge(group.getId());
    assertThat(challenge.getSubChallenges().size(), not(0));
    challenge.getSubChallenges().get(0).setRepetitions(2);

    AzkarChallenge createdChallenge = createGroupChallenge(user, challenge);
    for (SubChallenge subChallenge : createdChallenge.getSubChallenges()) {
      subChallenge.setRepetitions(1);
    }

    UpdateChallengeRequest requestBody = createUpdateChallengeRequest(createdChallenge);
    updateChallenge(user, createdChallenge.getId(), requestBody)
        .andExpect(status().isOk());

    User updatedUser = userRepo.findById(user.getId()).get();
    assertThat(updatedUser.getUserGroups().size(), is(userGroupsCountBefore + 1));

    UserGroup userGroup = updatedUser.getUserGroups().get(userGroupsCountBefore);
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
  protected AzkarChallenge getChallengeProgressFromApi(AzkarChallenge challenge)
      throws Exception {
    ResultActions resultActions = azkarApi.getChallenge(user, challenge.getId())
        .andExpect(status().isOk());
    return getResponse(resultActions, GetChallengeResponse.class).getData();
  }

  @Override
  protected AzkarChallenge createNewChallenge(User user) throws Exception {
    return createGroupChallenge(user, group.getId());
  }
}
