package com.azkar.controllers.challengecontroller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.controllers.ControllerTestBase;
import com.azkar.entities.Challenge;
import com.azkar.entities.Challenge.SubChallenges;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.entities.User.UserChallenge;
import com.azkar.factories.GroupFactory;
import com.azkar.factories.UserFactory;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.challengecontroller.requests.AddChallengeRequest;
import com.azkar.payload.challengecontroller.responses.AddChallengeResponse;
import com.azkar.payload.exceptions.BadRequestException;
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

public class GroupChallengeTest extends ControllerTestBase {

  private final static String CHALLENGE_NAME = "challenge_name";
  private final static String CHALLENGE_MOTIVATION = "challenge_motivation";
  private final static long EXPIRY_DATE_OFFSET = 60 * 60;
  private final static SubChallenges SUB_CHALLENGE = SubChallenges.builder()
      .zekr("zekr")
      .leftRepetitions(3)
      .originalRepetitions(3)
      .build();

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
    User anotherGroupMember = UserFactory.getNewUser();
    addNewUser(anotherGroupMember);
    User nonGroupMember = UserFactory.getNewUser();
    addNewUser(nonGroupMember);
    addUserToGroup(anotherGroupMember, /* invitingUser= */ user1, validGroup.getId());
    long expiryDate = Instant.now().getEpochSecond() + EXPIRY_DATE_OFFSET;
    Challenge challenge = Challenge.builder()
        .name(CHALLENGE_NAME)
        .motivation(CHALLENGE_MOTIVATION)
        .expiryDate(expiryDate)
        .subChallenges(ImmutableList.of(SUB_CHALLENGE))
        .groupId(validGroup.getId())
        .build();
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setData(challenge.toBuilder()
        .usersAccepted(ImmutableList.of(user1.getId()))
        .creatingUserId(user1.getId())
        .isOngoing(false)
        .usersFinished(new ArrayList<>())
        .build()
    );

    performPostRequest(user1, "/challenges", mapToJson(new AddChallengeRequest(challenge)))
        .andExpect(status().isOk())
        .andExpect(content().json(mapToJson(expectedResponse)));

    List<UserChallenge> userChallenges = userRepo.findById(user1.getId()).get().getUserChallenges();
    assertThat(userChallenges.size(), is(1));
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
    long expiryDate = Instant.now().getEpochSecond() + EXPIRY_DATE_OFFSET;
    Challenge challenge = Challenge.builder()
        .name(CHALLENGE_NAME)
        .motivation(CHALLENGE_MOTIVATION)
        .expiryDate(expiryDate)
        .subChallenges(ImmutableList.of(SUB_CHALLENGE))
        .groupId(validGroup.getId())
        .build();
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

    List<UserChallenge> userChallenges = userRepo.findById(user1.getId()).get().getUserChallenges();
    List<String> groupChallenges = groupRepo.findById(validGroup.getId()).get().getChallengesIds();
    assertThat(userChallenges.size(), is(1));
    assertThat(groupChallenges.size(), is(1));
  }

  @Test
  public void addChallenge_zeroSubChallengeRepetitions_shouldNotSucceed() throws Exception {
    long expiryDate = Instant.now().getEpochSecond() + EXPIRY_DATE_OFFSET;
    SubChallenges zeroRepetitionSubChallenge = SubChallenges.builder().zekr("zekr").build();
    Challenge challenge = Challenge.builder()
        .name(CHALLENGE_NAME)
        .motivation(CHALLENGE_MOTIVATION)
        .expiryDate(expiryDate)
        .subChallenges(ImmutableList.of(zeroRepetitionSubChallenge))
        .groupId(validGroup.getId())
        .build();
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setError(new Error(AddChallengeRequest.MALFORMED_SUB_CHALLENGES_ERROR));

    performPostRequest(user1, "/challenges", mapToJson(new AddChallengeRequest(challenge)))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(mapToJson(expectedResponse)));

    List<UserChallenge> userChallenges = userRepo.findById(user1.getId()).get().getUserChallenges();
    assertTrue("UserChallenges list is not empty.", userChallenges.isEmpty());
  }

  @Test
  public void addChallenge_invalidGroup_shouldNotSucceed() throws Exception {
    long expiryDate = Instant.now().getEpochSecond() + EXPIRY_DATE_OFFSET;
    Challenge challenge = Challenge.builder()
        .name(CHALLENGE_NAME)
        .motivation(CHALLENGE_MOTIVATION)
        .expiryDate(expiryDate)
        .subChallenges(ImmutableList.of(SUB_CHALLENGE))
        .groupId(invalidGroup.getId())
        .build();
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setError(new Error(AddChallengeRequest.GROUP_NOT_FOUND_ERROR));

    performPostRequest(user1, "/challenges", mapToJson(new AddChallengeRequest(challenge)))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(mapToJson(expectedResponse)));

    List<UserChallenge> userChallenges = userRepo.findById(user1.getId()).get().getUserChallenges();
    assertTrue("UserChallenges list is not empty.", userChallenges.isEmpty());
  }

  @Test
  public void addChallenge_missingMotivationField_shouldNotSucceed() throws Exception {
    long expiryDate = Instant.now().getEpochSecond() + EXPIRY_DATE_OFFSET;
    Challenge challenge = Challenge.builder()
        .name(CHALLENGE_NAME)
        .expiryDate(expiryDate)
        .subChallenges(ImmutableList.of(SUB_CHALLENGE))
        .groupId(validGroup.getId())
        .build();
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setError(new Error(BadRequestException.REQUIRED_FIELDS_NOT_GIVEN_ERROR));

    performPostRequest(user1, "/challenges", mapToJson(new AddChallengeRequest(challenge)))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(mapToJson(expectedResponse)));

    List<UserChallenge> userChallenges = userRepo.findById(user1.getId()).get().getUserChallenges();
    assertTrue("UserChallenges list is expected to be empty but it is not.",
        userChallenges.isEmpty());
    List<String> groupChallenges = groupRepo.findById(validGroup.getId()).get().getChallengesIds();
    assertTrue("GroupChallenges list is expected to be empty but it is not.",
        groupChallenges.isEmpty());
  }

  @Test
  public void addChallenge_pastExpiryDate_shouldNotSucceed() throws Exception {
    long expiryDate = Instant.now().getEpochSecond() - EXPIRY_DATE_OFFSET;
    Challenge challenge = Challenge.builder()
        .name(CHALLENGE_NAME)
        .motivation(CHALLENGE_MOTIVATION)
        .expiryDate(expiryDate)
        .subChallenges(ImmutableList.of(SUB_CHALLENGE))
        .groupId(validGroup.getId())
        .build();
    AddChallengeResponse expectedResponse = new AddChallengeResponse();
    expectedResponse.setError(new Error(AddChallengeRequest.PAST_EXPIRY_DATE_ERROR));

    performPostRequest(user1, "/challenges", mapToJson(new AddChallengeRequest(challenge)))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(mapToJson(expectedResponse)));

    List<UserChallenge> userChallenges = userRepo.findById(user1.getId()).get().getUserChallenges();
    assertTrue("UserChallenges list is not empty.", userChallenges.isEmpty());
    List<String> groupChallenges = groupRepo.findById(validGroup.getId()).get().getChallengesIds();
    assertTrue("GroupChallenges list is expected to be empty but it is not.",
        groupChallenges.isEmpty());
  }

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
}
