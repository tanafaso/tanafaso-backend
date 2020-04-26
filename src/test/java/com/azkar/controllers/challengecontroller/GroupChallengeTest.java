package com.azkar.controllers.challengecontroller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
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

  private final static String CHALLENGE_NAME = "challenge-name";
  private final static String CHALLENGE_MOTIVATION = "challenge-motivation";
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

  private User user1;
  private Group validGroup;
  private Group invalidGroup;

  @Before
  public void before() {
    user1 = UserFactory.getNewUser();
    addNewUser(user1);
    validGroup = GroupFactory.getNewGroup(user1.getId());
    groupRepo.save(validGroup);
    invalidGroup = GroupFactory.getNewGroup(user1.getId());
  }

  @Test
  public void addChallenge_normalScenario_shouldSucceed() throws Exception {
    long expiryDate = Instant.now().getEpochSecond() + EXPIRY_DATE_OFFSET;
    AddChallengeRequest request = AddChallengeRequest.builder()
        .name(CHALLENGE_NAME)
        .motivation(CHALLENGE_MOTIVATION)
        .expiryDate(expiryDate)
        .subChallenges(ImmutableList.of(SUB_CHALLENGE))
        .groupId(validGroup.getId())
        .build();
    AddChallengeResponse successResponse = new AddChallengeResponse();
    successResponse.setData(Challenge.builder()
        .name(request.getName())
        .motivation(request.getMotivation())
        .expiryDate(request.getExpiryDate())
        .subChallenges(request.getSubChallenges())
        .groupId(request.getGroupId())
        .usersAccepted(ImmutableList.of(user1.getId()))
        .creatingUserId(user1.getId())
        .isOngoing(false)
        .usersFinished(new ArrayList<>())
        .build()
    );

    performPostRequest(user1, "/challenges", mapToJson(request))
        .andExpect(status().isOk())
        .andExpect(content().json(mapToJson(successResponse)));

    List<UserChallenge> userChallenges = userRepo.findById(user1.getId()).get().getUserChallenges();
    List<String> groupChallenges = groupRepo.findById(validGroup.getId()).get().getChallengesIds();
    assertThat(userChallenges.size(), is(1));
    assertThat(groupChallenges.size(), is(1));
  }

  @Test
  public void addChallenge_invalidGroup_shouldFail() throws Exception {
    long expiryDate = Instant.now().getEpochSecond() + EXPIRY_DATE_OFFSET;
    AddChallengeRequest request = AddChallengeRequest.builder()
        .name(CHALLENGE_NAME)
        .motivation(CHALLENGE_MOTIVATION)
        .expiryDate(expiryDate)
        .subChallenges(ImmutableList.of(SUB_CHALLENGE))
        .groupId(invalidGroup.getId())
        .build();
    AddChallengeResponse failureResponse = new AddChallengeResponse();
    failureResponse.setError(new Error(AddChallengeRequest.GROUP_NOT_FOUND_ERROR));

    performPostRequest(user1, "/challenges", mapToJson(request))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(mapToJson(failureResponse)));

    List<UserChallenge> userChallenges = userRepo.findById(user1.getId()).get().getUserChallenges();
    assertThat(userChallenges.size(), is(0));
  }

  @Test
  public void addChallenge_missingMotivationField_shouldFail() throws Exception {
    long expiryDate = Instant.now().getEpochSecond() + EXPIRY_DATE_OFFSET;
    AddChallengeRequest request = AddChallengeRequest.builder()
        .name(CHALLENGE_NAME)
        .expiryDate(expiryDate)
        .subChallenges(ImmutableList.of(SUB_CHALLENGE))
        .groupId(validGroup.getId())
        .build();
    AddChallengeResponse failureResponse = new AddChallengeResponse();
    failureResponse.setError(new Error(BadRequestException.REQUIRED_FIELDS_NOT_GIVEN_ERROR));

    performPostRequest(user1, "/challenges", mapToJson(request))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(mapToJson(failureResponse)));

    List<UserChallenge> userChallenges = userRepo.findById(user1.getId()).get().getUserChallenges();
    assertThat(userChallenges.size(), is(0));
  }

  @Test
  public void addChallenge_pastExpiryDate_shouldFail() throws Exception {
    long expiryDate = Instant.now().getEpochSecond() - EXPIRY_DATE_OFFSET;
    AddChallengeRequest request = AddChallengeRequest.builder()
        .name(CHALLENGE_NAME)
        .motivation(CHALLENGE_MOTIVATION)
        .expiryDate(expiryDate)
        .subChallenges(ImmutableList.of(SUB_CHALLENGE))
        .groupId(validGroup.getId())
        .build();
    AddChallengeResponse failureResponse = new AddChallengeResponse();
    failureResponse.setError(new Error(AddChallengeRequest.PAST_EXPIRY_DATE_ERROR));

    performPostRequest(user1, "/challenges", mapToJson(request))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(mapToJson(failureResponse)));

    List<UserChallenge> userChallenges = userRepo.findById(user1.getId()).get().getUserChallenges();
    assertThat(userChallenges.size(), is(0));
  }
}
