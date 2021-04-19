package com.azkar.controllers.challengecontroller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
import com.azkar.entities.Zekr;
import com.azkar.factories.entities.ChallengeFactory;
import com.azkar.factories.entities.GroupFactory;
import com.azkar.factories.entities.UserFactory;
import com.azkar.payload.ResponseBase.Status;
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
    addUserToGroup(anotherGroupMember, /* invitingUser= */ user1, validGroup.getId());
    Challenge challenge = ChallengeFactory.getNewChallenge(validGroup.getId());
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setData(challenge.toBuilder()
        .creatingUserId(user1.getId())
        .usersFinished(new ArrayList<>())
        .build()
    );

    azkarApi.createChallenge(user1, challenge)
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
  public void addChallenge_oneMemberInGroup_shouldSucceed() throws Exception {
    Challenge challenge = ChallengeFactory.getNewChallenge(validGroup.getId());
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setData(challenge.toBuilder()
        .creatingUserId(user1.getId())
        .usersFinished(new ArrayList<>())
        .build()
    );

    azkarApi.createChallenge(user1, challenge)
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

    azkarApi.createChallenge(user1, challenge)
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

    azkarApi.createChallenge(user1, challenge)
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

    azkarApi.createChallenge(user1, challenge)
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

    azkarApi.createChallenge(nonGroupMember, challenge)
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
        .subChallenges(ImmutableList.of(ChallengeFactory.subChallenge1()))
        .groupId(validGroup.getId())
        .build();
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setData(challenge);

    azkarApi.createChallenge(user1, challenge)
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
        .subChallenges(ImmutableList.of(ChallengeFactory.subChallenge1()))
        .groupId(validGroup.getId())
        .build();
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setStatus(new Status(Status.PAST_EXPIRY_DATE_ERROR));

    azkarApi.createChallenge(user1, challenge)
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
    addUserToGroup(groupMember, /* invitingUser= */ user1, validGroup.getId());
    addNewValidChallenge(groupMember, CHALLENGE_NAME_PREFIX_2, validGroup.getId());

    GetChallengesResponse user1AllChallenges = getUserAllChallenges(user1);
    GetChallengesResponse groupMemberAllChallenges = getUserAllChallenges(groupMember);
    GetChallengesResponse nonGroupMemberAllChallenges = getUserAllChallenges(nonGroupMember);

    assertThat(user1AllChallenges.getData().size(), is(2));
    assertThat(user1AllChallenges.getData().get(0).getName(),
        startsWith(CHALLENGE_NAME_PREFIX_2));
    assertThat(user1AllChallenges.getData().get(1).getName(),
        startsWith(CHALLENGE_NAME_PREFIX_1));
    assertThat(groupMemberAllChallenges.getData().size(), is(2));
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

  private User createNewGroupMember(Group group) throws Exception {
    User newGroupMember = getNewRegisteredUser();
    User groupAdmin = userRepo.findById(group.getAdminId()).get();
    addUserToGroup(newGroupMember, groupAdmin, group.getId());
    return newGroupMember;
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

  // TODO: Reuse existing functions in GroupMembershipTest.
  private void addUserToGroup(User user, User invitingUser, String groupId)
      throws Exception {
    inviteUserToGroup(invitingUser, user, groupId);
    acceptInvitationToGroup(user, groupId);
  }

  private ResultActions inviteUserToGroup(User invitingUser, User invitedUser, String groupId)
      throws Exception {
    return performPutRequest(invitingUser, String.format("/groups/%s/invite/%s", groupId,
        invitedUser.getId()),
        /*body=*/ null);
  }

  private ResultActions acceptInvitationToGroup(User user, String groupId)
      throws Exception {
    return performPutRequest(user, String.format("/groups/%s/accept/", groupId), /*body=*/ null);
  }

  private ResultActions addNewValidChallenge(User creatingUser, String challengeNamePrefix,
      String groupId)
      throws Exception {
    Challenge challenge = ChallengeFactory.getNewChallenge(challengeNamePrefix, groupId);
    return azkarApi.createChallenge(creatingUser, challenge);
  }
}
