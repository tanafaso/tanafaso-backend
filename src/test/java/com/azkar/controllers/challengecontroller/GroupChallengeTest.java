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
import com.azkar.entities.Challenge;
import com.azkar.entities.Challenge.SubChallenges;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.entities.User.UserChallengeStatus;
import com.azkar.factories.entities.ChallengeFactory;
import com.azkar.factories.entities.GroupFactory;
import com.azkar.factories.entities.UserFactory;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.challengecontroller.requests.AddChallengeRequest;
import com.azkar.payload.challengecontroller.responses.AddChallengeResponse;
import com.azkar.payload.challengecontroller.responses.GetChallengesResponse;
import com.azkar.payload.exceptions.BadRequestException;
import com.azkar.repos.GroupRepo;
import com.azkar.repos.UserRepo;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

public class GroupChallengeTest extends TestBase {

  private static final String ONGOING_CHALLENGE_NAME_PREFIX = "ongoing_challenge";
  private static final String PROPOSED_CHALLENGE_NAME_PREFIX = "proposed_challenge";

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
        .usersAccepted(ImmutableList.of(user1.getId()))
        .creatingUserId(user1.getId())
        .isOngoing(false)
        .usersFinished(new ArrayList<>())
        .build()
    );

    performPostRequest(user1, "/challenges", /* body= */
        mapToJson(new AddChallengeRequest(challenge)))
        .andExpect(status().isOk())
        .andExpect(content().json(mapToJson(expectedResponse)));

    List<UserChallengeStatus> userChallengeStatuses = userRepo.findById(user1.getId()).get()
        .getUserChallengeStatuses();
    assertThat(userChallengeStatuses.size(), is(1));
    List<String> groupChallenges = groupRepo.findById(validGroup.getId()).get().getChallengesIds();
    assertThat(groupChallenges.size(), is(1));
    User updatedUser1 = userRepo.findById(user1.getId()).get();
    User updatedAnotherGroupMember = userRepo.findById(anotherGroupMember.getId()).get();
    User updatedNonGroupMember = userRepo.findById(nonGroupMember.getId()).get();
    assertThat(updatedUser1.getUserChallengeStatuses().size(), is(1));
    assertThat(updatedUser1.getUserChallengeStatuses().get(0).isOngoing(), is(false));
    assertThat(updatedAnotherGroupMember.getUserChallengeStatuses().size(), is(1));
    assertThat(updatedNonGroupMember.getUserChallengeStatuses().size(), is(0));
  }

  @Test
  public void addChallenge_oneMemberInGroup_shouldSucceed() throws Exception {
    Challenge challenge = ChallengeFactory.getNewChallenge(validGroup.getId());
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setData(challenge.toBuilder()
        .usersAccepted(ImmutableList.of(user1.getId()))
        .creatingUserId(user1.getId())
        .isOngoing(true)
        .usersFinished(new ArrayList<>())
        .build()
    );

    performPostRequest(user1, "/challenges", mapToJson(new AddChallengeRequest(challenge)))
        .andExpect(status().isOk())
        .andExpect(content().json(mapToJson(expectedResponse)));

    List<UserChallengeStatus> userChallengeStatuses = userRepo.findById(user1.getId()).get()
        .getUserChallengeStatuses();
    List<String> groupChallenges = groupRepo.findById(validGroup.getId()).get().getChallengesIds();
    assertThat(userChallengeStatuses.size(), is(1));
    assertThat(userChallengeStatuses.get(0).isOngoing(), is(true));
    assertThat(groupChallenges.size(), is(1));
  }

  @Test
  public void addChallenge_zeroSubChallengeRepetitions_shouldNotSucceed() throws Exception {
    long expiryDate = Instant.now().getEpochSecond() + ChallengeFactory.EXPIRY_DATE_OFFSET;
    SubChallenges zeroRepetitionSubChallenge = SubChallenges.builder().zekr("zekr").build();
    Challenge challenge = Challenge.builder()
        .name(ChallengeFactory.CHALLENGE_NAME_BASE)
        .motivation(ChallengeFactory.CHALLENGE_MOTIVATION)
        .expiryDate(expiryDate)
        .subChallenges(ImmutableList.of(zeroRepetitionSubChallenge))
        .groupId(validGroup.getId())
        .build();
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setError(new Error(AddChallengeRequest.MALFORMED_SUB_CHALLENGES_ERROR));

    performPostRequest(user1, "/challenges", mapToJson(new AddChallengeRequest(challenge)))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(mapToJson(expectedResponse)));

    List<UserChallengeStatus> userChallengeStatuses = userRepo.findById(user1.getId()).get()
        .getUserChallengeStatuses();
    assertTrue("UserChallenges list is not empty.", userChallengeStatuses.isEmpty());
  }

  @Test
  public void addChallenge_invalidGroup_shouldNotSucceed() throws Exception {
    Challenge challenge = ChallengeFactory.getNewChallenge(invalidGroup.getId());
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setError(new Error(AddChallengeResponse.GROUP_NOT_FOUND_ERROR));

    performPostRequest(user1, "/challenges", mapToJson(new AddChallengeRequest(challenge)))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(mapToJson(expectedResponse)));

    List<UserChallengeStatus> userChallengeStatuses = userRepo.findById(user1.getId()).get()
        .getUserChallengeStatuses();
    assertTrue("UserChallenges list is not empty.", userChallengeStatuses.isEmpty());
  }

  @Test
  public void addChallenge_nonGroupMember_shouldNotSucceed() throws Exception {
    Challenge challenge = ChallengeFactory.getNewChallenge(validGroup.getId());
    User nonGroupMember = UserFactory.getNewUser();
    addNewUser(nonGroupMember);
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setError(new Error(AddChallengeResponse.NOT_GROUP_MEMBER_ERROR));

    performPostRequest(nonGroupMember,
        "/challenges",
        mapToJson(new AddChallengeRequest(challenge)))
        .andExpect(status().isForbidden())
        .andExpect(content().json(mapToJson(expectedResponse)));

    List<UserChallengeStatus> userChallenges = userRepo.findById(nonGroupMember.getId())
        .get()
        .getUserChallengeStatuses();
    assertThat(userChallenges, empty());
    List<String> groupChallenges = groupRepo.findById(validGroup.getId()).get().getChallengesIds();
    assertThat(groupChallenges, empty());
  }

  @Test
  public void addChallenge_missingMotivationField_shouldNotSucceed() throws Exception {
    long expiryDate = Instant.now().getEpochSecond() + ChallengeFactory.EXPIRY_DATE_OFFSET;
    Challenge challenge = Challenge.builder()
        .name(ChallengeFactory.CHALLENGE_NAME_BASE)
        .expiryDate(expiryDate)
        .subChallenges(ImmutableList.of(ChallengeFactory.SUB_CHALLENGE))
        .groupId(validGroup.getId())
        .build();
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setError(new Error(BadRequestException.REQUIRED_FIELDS_NOT_GIVEN_ERROR));

    performPostRequest(user1, "/challenges", mapToJson(new AddChallengeRequest(challenge)))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(mapToJson(expectedResponse)));

    List<UserChallengeStatus> userChallengeStatuses = userRepo.findById(user1.getId()).get()
        .getUserChallengeStatuses();
    assertTrue("UserChallenges list is expected to be empty but it is not.",
        userChallengeStatuses.isEmpty());
    List<String> groupChallenges = groupRepo.findById(validGroup.getId()).get().getChallengesIds();
    assertTrue("GroupChallenges list is expected to be empty but it is not.",
        groupChallenges.isEmpty());
  }

  @Test
  public void addChallenge_pastExpiryDate_shouldNotSucceed() throws Exception {
    long pastExpiryDate = Instant.now().getEpochSecond() - ChallengeFactory.EXPIRY_DATE_OFFSET;
    Challenge challenge = Challenge.builder()
        .name(ChallengeFactory.CHALLENGE_NAME_BASE)
        .motivation(ChallengeFactory.CHALLENGE_MOTIVATION)
        .expiryDate(pastExpiryDate)
        .subChallenges(ImmutableList.of(ChallengeFactory.SUB_CHALLENGE))
        .groupId(validGroup.getId())
        .build();
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setError(new Error(AddChallengeRequest.PAST_EXPIRY_DATE_ERROR));

    performPostRequest(user1, "/challenges", mapToJson(new AddChallengeRequest(challenge)))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(mapToJson(expectedResponse)));

    List<UserChallengeStatus> userChallengeStatuses = userRepo.findById(user1.getId()).get()
        .getUserChallengeStatuses();
    assertTrue("UserChallenges list is not empty.", userChallengeStatuses.isEmpty());
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
    addNewValidChallenge(user1, ONGOING_CHALLENGE_NAME_PREFIX, validGroup.getId());
    addUserToGroup(groupMember, /* invitingUser= */ user1, validGroup.getId());
    addNewValidChallenge(groupMember, PROPOSED_CHALLENGE_NAME_PREFIX, validGroup.getId());

    GetChallengesResponse user1OngoingChallenges = getUserOngoingChallenges(user1);
    GetChallengesResponse user1ProposedChallenges = getUserProposedChallenges(user1);
    GetChallengesResponse groupMemberOngoingChallenges = getUserOngoingChallenges(groupMember);
    GetChallengesResponse groupMemberProposedChallenges = getUserProposedChallenges(groupMember);
    GetChallengesResponse nonGroupMemberOngoingChallenges = getUserOngoingChallenges(
        nonGroupMember);
    GetChallengesResponse nonGroupMemberProposedChallenges = getUserProposedChallenges(
        nonGroupMember);

    assertThat(user1OngoingChallenges.getData().size(), is(1));
    assertThat(user1ProposedChallenges.getData().size(), is(1));
    assertThat(user1OngoingChallenges.getData().get(0).getChallengeInfo().getName(),
        startsWith(ONGOING_CHALLENGE_NAME_PREFIX));
    assertThat(user1ProposedChallenges.getData().get(0).getChallengeInfo().getName(),
        startsWith(PROPOSED_CHALLENGE_NAME_PREFIX));
    // TODO(issue#62): groupMemberOngoingChallenges should have size 1 when the issue is solved.
    assertThat(groupMemberOngoingChallenges.getData().size(), is(0));
    assertThat(groupMemberProposedChallenges.getData().size(), is(1));
    assertThat(groupMemberProposedChallenges.getData().get(0).getChallengeInfo().getName(),
        startsWith(PROPOSED_CHALLENGE_NAME_PREFIX));
    assertThat(nonGroupMemberOngoingChallenges.getData().size(), is(0));
    assertThat(nonGroupMemberProposedChallenges.getData().size(), is(0));
  }


  @Test
  public void getGroupChallenges_invalidGroup_shouldFail() throws Exception {
    GetChallengesResponse expectedResponse = new GetChallengesResponse();
    expectedResponse.setError(new Error(GetChallengesResponse.GROUP_NOT_FOUND_ERROR));

    performGetRequest(user1,
        String.format("/challenges/groups/%s/ongoing/", invalidGroup.getId()))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().json(mapToJson(expectedResponse)));

    performGetRequest(user1,
        String.format("/challenges/groups/%s/proposed/", invalidGroup.getId()))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().json(mapToJson(expectedResponse)));
  }

  @Test
  public void getGroupChallenges_nonGroupMember_shouldFail() throws Exception {
    User nonGroupMember = getNewRegisteredUser();
    GetChallengesResponse expectedResponse = new GetChallengesResponse();
    expectedResponse.setError(new Error(GetChallengesResponse.NON_GROUP_MEMBER_ERROR));

    performGetRequest(nonGroupMember,
        String.format("/challenges/groups/%s/ongoing/", validGroup.getId()))
        .andExpect(status().isForbidden())
        .andExpect(content().json(mapToJson(expectedResponse)));

    performGetRequest(nonGroupMember,
        String.format("/challenges/groups/%s/proposed/", validGroup.getId()))
        .andExpect(status().isForbidden())
        .andExpect(content().json(mapToJson(expectedResponse)));
  }

  @Test
  public void getGroupChallenges_oneOngoingOneProposedChallenge_shouldSucceed() throws Exception {
    // TODO(issue#92): A hacky way to add ongoing challenge is to insert a challenge in group
    //  with only one member. This should be changed when accept challenge functionality is added.
    addNewValidChallenge(user1, ONGOING_CHALLENGE_NAME_PREFIX, validGroup.getId());

    User groupMember = createNewGroupMember(validGroup);
    addNewValidChallenge(groupMember, PROPOSED_CHALLENGE_NAME_PREFIX, validGroup.getId());

    GetChallengesResponse groupOngoingChallenges = getGroupOngoingChallenges(user1,
        validGroup.getId());
    GetChallengesResponse groupProposedChallenges = getGroupProposedChallenges(user1,
        validGroup.getId());

    assertThat(
        Iterables.getOnlyElement(groupOngoingChallenges.getData()).getChallengeInfo().getName(),
        startsWith(ONGOING_CHALLENGE_NAME_PREFIX));
    assertThat(
        Iterables.getOnlyElement(groupProposedChallenges.getData()).getChallengeInfo().getName(),
        startsWith(PROPOSED_CHALLENGE_NAME_PREFIX));
  }

  @Test
  public void getGroupChallenges_multipleGroups_shouldSucceed() throws Exception {
    Group anotherGroup = GroupFactory.getNewGroup(user1.getId());
    groupRepo.save(anotherGroup);
    addNewValidChallenge(user1, /* challengeNamePrefix= */"", validGroup.getId());

    GetChallengesResponse validGroupOngoingChallenges = getGroupOngoingChallenges(user1,
        validGroup.getId());
    GetChallengesResponse anotherGroupOngoingChallenges = getGroupOngoingChallenges(user1,
        anotherGroup.getId());

    assertThat(validGroupOngoingChallenges.getData(), hasSize(1));
    assertThat(anotherGroupOngoingChallenges.getData(), empty());
  }

  private User createNewGroupMember(Group group) throws Exception {
    User newGroupMember = getNewRegisteredUser();
    User groupAdmin = userRepo.findById(group.getAdminId()).get();
    addUserToGroup(newGroupMember, groupAdmin, group.getId());
    return newGroupMember;
  }

  private GetChallengesResponse getGroupOngoingChallenges(User user, String groupId)
      throws Exception {
    return getGroupChallenges(user, groupId, /* isOngoing= */ true);
  }

  private GetChallengesResponse getGroupProposedChallenges(User user, String groupId)
      throws Exception {
    return getGroupChallenges(user, groupId, /* isOngoing= */ false);
  }

  private GetChallengesResponse getGroupChallenges(User user, String groupId, boolean isOngoing)
      throws Exception {
    String challengeStatus = isOngoing ? "ongoing" : "proposed";
    String responseJson =
        performGetRequest(user,
            String.format("/challenges/groups/%s/%s/", groupId, challengeStatus))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    return mapFromJson(responseJson, GetChallengesResponse.class);
  }

  private GetChallengesResponse getUserOngoingChallenges(User user) throws Exception {
    return getUserChallenges(user, /* isOngoing= */true);
  }

  private GetChallengesResponse getUserProposedChallenges(User user) throws Exception {
    return getUserChallenges(user, /* isOngoing= */false);
  }

  private GetChallengesResponse getUserChallenges(User user, boolean isOngoing) throws Exception {
    String responseJson = performGetRequest(user,
        "/challenges/" + (isOngoing ? "ongoing" : "proposed"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    return mapFromJson(responseJson, GetChallengesResponse.class);
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
    return performPostRequest(creatingUser, "/challenges",
        mapToJson(new AddChallengeRequest(challenge)));
  }
}
