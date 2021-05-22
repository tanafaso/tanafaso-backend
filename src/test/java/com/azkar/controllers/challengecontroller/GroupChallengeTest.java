package com.azkar.controllers.challengecontroller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.TestBase;
import com.azkar.controllers.utils.JsonHandler;
import com.azkar.entities.Challenge;
import com.azkar.entities.Challenge.SubChallenge;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.entities.User.UserGroup;
import com.azkar.entities.Zekr;
import com.azkar.factories.entities.ChallengeFactory;
import com.azkar.factories.entities.GroupFactory;
import com.azkar.factories.entities.UserFactory;
import com.azkar.payload.ResponseBase.Status;
import com.azkar.payload.challengecontroller.requests.AddFriendsChallengeRequest;
import com.azkar.payload.challengecontroller.responses.AddChallengeResponse;
import com.azkar.payload.challengecontroller.responses.GetChallengesResponse;
import com.azkar.repos.GroupRepo;
import com.azkar.repos.UserRepo;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

public class GroupChallengeTest extends TestBase {

  private static final String CHALLENGE_NAME_PREFIX_1 = "challenge_1_";
  private static final String CHALLENGE_NAME_PREFIX_2 = "challenge_2_";

  @Autowired
  GroupRepo groupRepo;
  @Autowired
  UserRepo userRepo;

  private User user1 = UserFactory.getNewUser();
  private Group validGroup = GroupFactory.getNewGroup(user1.getId());
  private Group invalidGroup = GroupFactory.getNewGroup(user1.getId());

  @Before
  public void before() {
    addNewUser(user1);
    groupRepo.save(validGroup);
  }

  @Test
  public void addChallenge_multipleMembersInGroup_shouldSucceed() throws Exception {
    User anotherGroupMember = getNewRegisteredUser();
    User nonGroupMember = getNewRegisteredUser();

    azkarApi.makeFriends(user1, anotherGroupMember);
    azkarApi.addUserToGroup(/* invitingUser= */ user1, anotherGroupMember, validGroup.getId());
    Challenge challenge = ChallengeFactory.getNewChallenge(validGroup.getId());
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setData(challenge.toBuilder()
        .creatingUserId(user1.getId())
        .usersFinished(new ArrayList<>())
        .build()
    );

    azkarApi.addChallenge(user1, challenge)
        .andExpect(status().isOk())
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    List<Challenge> challengesProgress = userRepo.findById(user1.getId()).get()
        .getUserChallenges();
    assertThat(challengesProgress.size(), is(1));
    List<String> groupChallenges = groupRepo.findById(validGroup.getId()).get().getChallengesIds();
    assertThat(groupChallenges.size(), is(1));
    User updatedUser1 = userRepo.findById(user1.getId()).get();
    User updatedAnotherGroupMember = userRepo.findById(anotherGroupMember.getId()).get();
    User updatedNonGroupMember = userRepo.findById(nonGroupMember.getId()).get();
    assertThat(updatedUser1.getUserChallenges().size(), is(1));
    assertThat(updatedAnotherGroupMember.getUserChallenges().size(), is(1));
    assertThat(updatedNonGroupMember.getUserChallenges().size(), is(0));
  }

  @Test
  public void addFriendsChallenge_normalScenario_shouldSucceed() throws Exception {
    User user2 = getNewRegisteredUser();
    User user3 = getNewRegisteredUser();

    azkarApi.makeFriends(user1, user2);
    azkarApi.makeFriends(user1, user3);

    int user1GroupsNumBefore = userRepo.findById(user1.getId()).get().getUserGroups().size();
    int user2GroupsNumBefore = userRepo.findById(user2.getId()).get().getUserGroups().size();
    int user3GroupsNumBefore = userRepo.findById(user3.getId()).get().getUserGroups().size();
    long groupsNumBefore = groupRepo.count();

    List<String> friendsIds = ImmutableList.of(user2.getId(), user3.getId());
    Challenge challenge = ChallengeFactory.getNewChallenge("toBeRemovedGroupId").toBuilder()
        .groupId(null)
        .build();

    AddFriendsChallengeRequest request =
        AddFriendsChallengeRequest.AddFriendsChallengeRequestBuilder().
            friendsIds(friendsIds)
            .challenge(challenge)
            .build();
    MvcResult result = azkarApi.addFriendsChallenge(user1, request)
        .andExpect(status().isOk())
        .andReturn();
    AddChallengeResponse response = JsonHandler.fromJson(result.getResponse().getContentAsString(),
        AddChallengeResponse.class);
    Challenge resultChallenge = response.getData();

    assertThat(groupRepo.count(), is(groupsNumBefore + 1));

    assertThat(resultChallenge.getName(), equalTo(challenge.getName()));
    assertThat(resultChallenge.getGroupId(), notNullValue());

    User updatedUser1 = userRepo.findById(user1.getId()).get();
    User updatedUser2 = userRepo.findById(user2.getId()).get();
    User updatedUser3 = userRepo.findById(user3.getId()).get();
    assertThat(updatedUser1.getUserGroups().size(), is(user1GroupsNumBefore + 1));
    assertThat(updatedUser2.getUserGroups().size(), is(user2GroupsNumBefore + 1));
    assertThat(updatedUser3.getUserGroups().size(), is(user3GroupsNumBefore + 1));

    UserGroup user1AddedGroup = updatedUser1.getUserGroups().get(user1GroupsNumBefore + 0);
    UserGroup user2AddedGroup = updatedUser2.getUserGroups().get(user2GroupsNumBefore + 0);
    UserGroup user3AddedGroup = updatedUser3.getUserGroups().get(user3GroupsNumBefore + 0);
    assertThat(user1AddedGroup.getGroupId(), equalTo(user2AddedGroup.getGroupId()));
    assertThat(user2AddedGroup.getGroupId(), equalTo(user3AddedGroup.getGroupId()));

    assertThat(user1AddedGroup.getInvitingUserId(), equalTo(user1.getId()));
    assertThat(user2AddedGroup.getInvitingUserId(), equalTo(user1.getId()));
    assertThat(user3AddedGroup.getInvitingUserId(), equalTo(user1.getId()));

    assertThat(user1AddedGroup.getGroupName(), nullValue());

    assertThat(updatedUser1.getUserChallenges().size(), is(1));
    assertThat(updatedUser2.getUserChallenges().size(), is(1));
    assertThat(updatedUser3.getUserChallenges().size(), is(1));

    assertThat(updatedUser1.getUserChallenges().get(0).getGroupId(),
        equalTo(user1AddedGroup.getGroupId()));
    assertThat(updatedUser2.getUserChallenges().get(0).getGroupId(),
        equalTo(user1AddedGroup.getGroupId()));
    assertThat(updatedUser3.getUserChallenges().get(0).getGroupId(),
        equalTo(user1AddedGroup.getGroupId()));

    assertThat(updatedUser1.getUserChallenges().get(0).getName(),
        equalTo(challenge.getName()));

    Group updatedGroup = groupRepo.findById(user1AddedGroup.getGroupId()).get();
    assertThat(updatedGroup.getUsersIds().size(), is(3));
    assertThat(updatedGroup.getUsersIds(),
        containsInAnyOrder(user1.getId(), user2.getId(), user3.getId()));
  }

  @Test
  public void addFriendsChallenge_notFriend_shouldFail() throws Exception {
    User user2 = getNewRegisteredUser();
    User user3 = getNewRegisteredUser();

    azkarApi.makeFriends(user1, user2);

    int user1GroupsNumBefore = userRepo.findById(user1.getId()).get().getUserGroups().size();
    int user2GroupsNumBefore = userRepo.findById(user2.getId()).get().getUserGroups().size();
    int user3GroupsNumBefore = userRepo.findById(user3.getId()).get().getUserGroups().size();
    long groupsNumBefore = groupRepo.count();

    List<String> friendsIds = ImmutableList.of(user2.getId(), user3.getId());
    Challenge challenge = ChallengeFactory.getNewChallenge("toBeRemovedGroupId").toBuilder()
        .groupId(null)
        .build();

    AddFriendsChallengeRequest request =
        AddFriendsChallengeRequest.AddFriendsChallengeRequestBuilder().
            friendsIds(friendsIds)
            .challenge(challenge)
            .build();
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setStatus(new Status(Status.NO_FRIENDSHIP_ERROR));
    azkarApi.addFriendsChallenge(user1, request)
        .andExpect(status().isBadRequest())
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    assertThat(groupRepo.count(), is(groupsNumBefore));

    User updatedUser1 = userRepo.findById(user1.getId()).get();
    User updatedUser2 = userRepo.findById(user2.getId()).get();
    User updatedUser3 = userRepo.findById(user3.getId()).get();
    assertThat(updatedUser1.getUserGroups().size(), is(user1GroupsNumBefore));
    assertThat(updatedUser2.getUserGroups().size(), is(user2GroupsNumBefore));
    assertThat(updatedUser3.getUserGroups().size(), is(user3GroupsNumBefore));

    assertThat(updatedUser1.getUserChallenges().size(), is(0));
    assertThat(updatedUser2.getUserChallenges().size(), is(0));
    assertThat(updatedUser3.getUserChallenges().size(), is(0));
  }

  @Test
  public void addFriendsChallenge_oneFriendProvided_shouldFail() throws Exception {
    User user2 = getNewRegisteredUser();

    azkarApi.makeFriends(user1, user2);

    int user1GroupsNumBefore = userRepo.findById(user1.getId()).get().getUserGroups().size();
    int user2GroupsNumBefore = userRepo.findById(user2.getId()).get().getUserGroups().size();
    long groupsNumBefore = groupRepo.count();

    List<String> friendsIds = ImmutableList.of(user2.getId());
    Challenge challenge = ChallengeFactory.getNewChallenge("toBeRemovedGroupId").toBuilder()
        .groupId(null)
        .build();

    AddFriendsChallengeRequest request =
        AddFriendsChallengeRequest.AddFriendsChallengeRequestBuilder().
            friendsIds(friendsIds)
            .challenge(challenge)
            .build();
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setStatus(new Status(Status.LESS_THAN_TWO_FRIENDS_ARE_PROVIDED_ERROR));
    azkarApi.addFriendsChallenge(user1, request)
        .andExpect(status().isBadRequest())
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    assertThat(groupRepo.count(), is(groupsNumBefore));

    User updatedUser1 = userRepo.findById(user1.getId()).get();
    User updatedUser2 = userRepo.findById(user2.getId()).get();
    assertThat(updatedUser1.getUserGroups().size(), is(user1GroupsNumBefore));
    assertThat(updatedUser2.getUserGroups().size(), is(user2GroupsNumBefore));

    assertThat(updatedUser1.getUserChallenges().size(), is(0));
    assertThat(updatedUser2.getUserChallenges().size(), is(0));
  }

  @Test
  public void addFriendsChallenge_duplicateFriendIds_shouldFail() throws Exception {
    User user2 = getNewRegisteredUser();

    azkarApi.makeFriends(user1, user2);

    int user1GroupsNumBefore = userRepo.findById(user1.getId()).get().getUserGroups().size();
    int user2GroupsNumBefore = userRepo.findById(user2.getId()).get().getUserGroups().size();
    long groupsNumBefore = groupRepo.count();

    List<String> friendsIds = ImmutableList.of(user2.getId(), user2.getId());
    Challenge challenge = ChallengeFactory.getNewChallenge("toBeRemovedGroupId").toBuilder()
        .groupId(null)
        .build();

    AddFriendsChallengeRequest request =
        AddFriendsChallengeRequest.AddFriendsChallengeRequestBuilder().
            friendsIds(friendsIds)
            .challenge(challenge)
            .build();
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setStatus(new Status(Status.DUPLICATE_FRIEND_IDS_PROVIDED_ERROR));
    azkarApi.addFriendsChallenge(user1, request)
        .andExpect(status().isBadRequest())
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    assertThat(groupRepo.count(), is(groupsNumBefore));

    User updatedUser1 = userRepo.findById(user1.getId()).get();
    User updatedUser2 = userRepo.findById(user2.getId()).get();
    assertThat(updatedUser1.getUserGroups().size(), is(user1GroupsNumBefore));
    assertThat(updatedUser2.getUserGroups().size(), is(user2GroupsNumBefore));

    assertThat(updatedUser1.getUserChallenges().size(), is(0));
    assertThat(updatedUser2.getUserChallenges().size(), is(0));
  }

  @Test
  public void addChallenge_oneMemberInGroup_shouldSucceed() throws Exception {
    Challenge challenge = ChallengeFactory.getNewChallenge(validGroup.getId());
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setData(challenge.toBuilder()
        .creatingUserId(user1.getId())
        .usersFinished(new ArrayList<>())
        .build()
    );

    azkarApi.addChallenge(user1, challenge)
        .andExpect(status().isOk())
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    List<Challenge> challengesProgress = userRepo.findById(user1.getId()).get()
        .getUserChallenges();
    List<String> groupChallenges = groupRepo.findById(validGroup.getId()).get().getChallengesIds();
    assertThat(challengesProgress.size(), is(1));
    assertThat(groupChallenges.size(), is(1));
  }

  @Test
  public void addChallenge_zeroSubChallengeRepetitions_shouldNotSucceed() throws Exception {
    long expiryDate = Instant.now().getEpochSecond() + ChallengeFactory.EXPIRY_DATE_OFFSET;
    SubChallenge zeroRepetitionSubChallenge =
        SubChallenge.builder().zekr(Zekr.builder().id(1).zekr("zekr").build())
            .build();
    Challenge challenge = Challenge.builder()
        .name(ChallengeFactory.CHALLENGE_NAME_BASE)
        .motivation(ChallengeFactory.CHALLENGE_MOTIVATION)
        .expiryDate(expiryDate)
        .subChallenges(ImmutableList.of(zeroRepetitionSubChallenge))
        .groupId(validGroup.getId())
        .build();
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setStatus(new Status(Status.MALFORMED_SUB_CHALLENGES_ERROR));

    azkarApi.addChallenge(user1, challenge)
        .andExpect(status().isBadRequest())
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    List<Challenge> challengesProgress = userRepo.findById(user1.getId()).get()
        .getUserChallenges();
    assertTrue("Challenges progress list is not empty.", challengesProgress.isEmpty());
  }

  @Test
  public void addChallenge_duplicateZekr_shouldNotSucceed() throws Exception {
    long expiryDate = Instant.now().getEpochSecond() + ChallengeFactory.EXPIRY_DATE_OFFSET;
    SubChallenge subChallenge1 =
        SubChallenge.builder().repetitions(2).zekr(Zekr.builder().id(1).zekr("zekr").build())
            .build();
    SubChallenge subChallenge2 =
        SubChallenge.builder().repetitions(3).zekr(Zekr.builder().id(1).zekr("zekr").build())
            .build();
    Challenge challenge = Challenge.builder()
        .name(ChallengeFactory.CHALLENGE_NAME_BASE)
        .motivation(ChallengeFactory.CHALLENGE_MOTIVATION)
        .expiryDate(expiryDate)
        .subChallenges(ImmutableList.of(subChallenge1, subChallenge2))
        .groupId(validGroup.getId())
        .build();
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setStatus(new Status(Status.CHALLENGE_CREATION_DUPLICATE_ZEKR_ERROR));

    azkarApi.addChallenge(user1, challenge)
        .andExpect(status().isBadRequest())
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    List<Challenge> challengesProgress = userRepo.findById(user1.getId()).get()
        .getUserChallenges();
    assertTrue("Challenges progress list is not empty.", challengesProgress.isEmpty());
  }

  @Test
  public void addChallenge_invalidGroup_shouldNotSucceed() throws Exception {
    Challenge challenge = ChallengeFactory.getNewChallenge(invalidGroup.getId());
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setStatus(new Status(Status.GROUP_NOT_FOUND_ERROR));

    azkarApi.addChallenge(user1, challenge)
        .andExpect(status().isBadRequest())
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    List<Challenge> challengesProgress = userRepo.findById(user1.getId()).get()
        .getUserChallenges();
    assertTrue("UserChallenges list is not empty.", challengesProgress.isEmpty());
  }

  @Test
  public void addChallenge_nonGroupMember_shouldNotSucceed() throws Exception {
    Challenge challenge = ChallengeFactory.getNewChallenge(validGroup.getId());
    User nonGroupMember = UserFactory.getNewUser();
    addNewUser(nonGroupMember);
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setStatus(new Status(Status.NOT_GROUP_MEMBER_ERROR));

    azkarApi.addChallenge(nonGroupMember, challenge)
        .andExpect(status().isForbidden())
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    List<Challenge> userChallenges = userRepo.findById(nonGroupMember.getId())
        .get()
        .getUserChallenges();
    assertThat(userChallenges, empty());
    List<String> groupChallenges = groupRepo.findById(validGroup.getId()).get().getChallengesIds();
    assertThat(groupChallenges, empty());
  }

  @Test
  public void addChallenge_missingMotivationField_shouldSucceed() throws Exception {
    long expiryDate = Instant.now().getEpochSecond() + ChallengeFactory.EXPIRY_DATE_OFFSET;
    Challenge challenge = Challenge.builder()
        .name(ChallengeFactory.CHALLENGE_NAME_BASE)
        .expiryDate(expiryDate)
        .subChallenges(
            ImmutableList.of(ChallengeFactory.subChallenge1()))
        .groupId(validGroup.getId())
        .build();
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setData(challenge);

    azkarApi.addChallenge(user1, challenge)
        .andExpect(status().isOk())
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    List<Challenge> challengesProgress = userRepo.findById(user1.getId()).get()
        .getUserChallenges();
    List<String> groupChallenges = groupRepo.findById(validGroup.getId()).get().getChallengesIds();
    assertThat(challengesProgress.size(), is(1));
    assertThat(groupChallenges.size(), is(1));
  }

  @Test
  public void addChallenge_pastExpiryDate_shouldNotSucceed() throws Exception {
    long pastExpiryDate = Instant.now().getEpochSecond() - ChallengeFactory.EXPIRY_DATE_OFFSET;
    Challenge challenge = Challenge.builder()
        .name(ChallengeFactory.CHALLENGE_NAME_BASE)
        .motivation(ChallengeFactory.CHALLENGE_MOTIVATION)
        .expiryDate(pastExpiryDate)
        .subChallenges(
            ImmutableList.of(ChallengeFactory.subChallenge1()))
        .groupId(validGroup.getId())
        .build();
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setStatus(new Status(Status.PAST_EXPIRY_DATE_ERROR));

    azkarApi.addChallenge(user1, challenge)
        .andExpect(status().isBadRequest())
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    List<Challenge> challengesProgress = userRepo.findById(user1.getId()).get()
        .getUserChallenges();
    assertTrue("UserChallenges list is not empty.", challengesProgress.isEmpty());
    List<String> groupChallenges = groupRepo.findById(validGroup.getId()).get().getChallengesIds();
    assertTrue("GroupChallenges list is expected to be empty but it is not.",
        groupChallenges.isEmpty());
  }

  @Test
  public void getChallenges_normalScenario_shouldSucceed() throws Exception {
    User groupMember = UserFactory.getNewUser();
    User nonGroupMember = UserFactory.getNewUser();
    addNewUser(groupMember);
    addNewUser(nonGroupMember);
    addNewValidChallenge(user1, CHALLENGE_NAME_PREFIX_1, validGroup.getId());
    azkarApi.makeFriends(user1, groupMember);
    azkarApi.addUserToGroup(/* invitingUser= */ user1, groupMember, validGroup.getId());
    addNewValidChallenge(groupMember, CHALLENGE_NAME_PREFIX_2, validGroup.getId());

    GetChallengesResponse user1AllChallenges = getUserAllChallenges(user1);
    GetChallengesResponse groupMemberAllChallenges = getUserAllChallenges(groupMember);
    GetChallengesResponse nonGroupMemberAllChallenges = getUserAllChallenges(nonGroupMember);

    assertThat(user1AllChallenges.getData().size(), is(2));
    assertThat(user1AllChallenges.getData().get(0).getName(),
        startsWith(CHALLENGE_NAME_PREFIX_2));
    assertThat(user1AllChallenges.getData().get(1).getName(),
        startsWith(CHALLENGE_NAME_PREFIX_1));
    // Currently new group members will only see new challenges.
    assertThat(groupMemberAllChallenges.getData().size(), is(1));
    assertThat(nonGroupMemberAllChallenges.getData().size(), is(0));
  }


  @Test
  public void getGroupChallenges_invalidGroup_shouldFail() throws Exception {
    GetChallengesResponse expectedResponse = new GetChallengesResponse();
    expectedResponse.setStatus(new Status(Status.GROUP_NOT_FOUND_ERROR));

    azkarApi.getAllChallengesInGroup(user1, invalidGroup.getId())
        .andExpect(status().isBadRequest())
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void getGroupChallenges_nonGroupMember_shouldFail() throws Exception {
    User nonGroupMember = getNewRegisteredUser();
    GetChallengesResponse expectedResponse = new GetChallengesResponse();
    expectedResponse.setStatus(new Status(Status.NON_GROUP_MEMBER_ERROR));

    azkarApi.getAllChallengesInGroup(nonGroupMember, validGroup.getId())
        .andExpect(status().isForbidden())
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void getGroupChallenges_multipleGroups_shouldSucceed() throws Exception {
    Group anotherGroup = GroupFactory.getNewGroup(user1.getId());
    groupRepo.save(anotherGroup);
    addNewValidChallenge(user1, /* challengeNamePrefix= */"", validGroup.getId());

    GetChallengesResponse validGroupAllChallenges = getAllChallengesInGroup(user1,
        validGroup.getId());
    GetChallengesResponse anotherGroupAllChallenges = getAllChallengesInGroup(user1,
        anotherGroup.getId());

    assertThat(validGroupAllChallenges.getData(), hasSize(1));
    assertThat(anotherGroupAllChallenges.getData(), empty());
  }

  private GetChallengesResponse getAllChallengesInGroup(User user, String groupId)
      throws Exception {
    ResultActions resultActions = azkarApi.getAllChallengesInGroup(user, groupId)
        .andExpect(status().isOk());
    return getResponse(resultActions, GetChallengesResponse.class);
  }

  private GetChallengesResponse getUserAllChallenges(User user) throws Exception {
    ResultActions resultActions = azkarApi.getAllChallenges(user).andExpect(status().isOk());
    return getResponse(resultActions, GetChallengesResponse.class);
  }

  private ResultActions addNewValidChallenge(User creatingUser, String challengeNamePrefix,
      String groupId)
      throws Exception {
    Challenge challenge = ChallengeFactory.getNewChallenge(challengeNamePrefix, groupId);
    return azkarApi.addChallenge(creatingUser, challenge);
  }
}
