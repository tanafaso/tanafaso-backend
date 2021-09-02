package com.azkar.controllers.challengecontroller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.TestBase;
import com.azkar.controllers.utils.JsonHandler;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.entities.User.UserGroup;
import com.azkar.entities.challenges.ReadingQuranChallenge;
import com.azkar.entities.challenges.ReadingQuranChallenge.SubChallenge;
import com.azkar.factories.entities.ChallengeFactory;
import com.azkar.factories.entities.GroupFactory;
import com.azkar.factories.entities.UserFactory;
import com.azkar.payload.ResponseBase.Status;
import com.azkar.payload.challengecontroller.requests.AddReadingQuranChallengeRequest;
import com.azkar.payload.challengecontroller.responses.AddReadingQuranChallengeResponse;
import com.azkar.repos.GroupRepo;
import com.azkar.repos.UserRepo;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

public class ReadingQuranChallengeTest extends TestBase {

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
  public void addReadingQuranChallenge_normalScenario_shouldSucceed() throws Exception {
    User user2 = getNewRegisteredUser();
    User user3 = getNewRegisteredUser();

    azkarApi.makeFriends(user1, user2);
    azkarApi.makeFriends(user1, user3);

    int user1GroupsNumBefore = userRepo.findById(user1.getId()).get().getUserGroups().size();
    int user2GroupsNumBefore = userRepo.findById(user2.getId()).get().getUserGroups().size();
    int user3GroupsNumBefore = userRepo.findById(user3.getId()).get().getUserGroups().size();
    long groupsNumBefore = groupRepo.count();

    List<String> friendsIds = ImmutableList.of(user2.getId(), user3.getId());
    ReadingQuranChallenge challenge =
        ChallengeFactory.getNewReadingChallenge("toBeRemovedGroupId").toBuilder()
            .groupId(null)
            .build();

    AddReadingQuranChallengeRequest request =
        AddReadingQuranChallengeRequest.AddReadingQuranChallengeRequestBuilder().
            friendsIds(friendsIds)
            .challenge(challenge)
            .build();
    MvcResult result = azkarApi.addReadingQuranChallenge(user1, request)
        .andExpect(status().isOk())
        .andReturn();
    AddReadingQuranChallengeResponse response =
        JsonHandler.fromJson(result.getResponse().getContentAsString(),
            AddReadingQuranChallengeResponse.class);
    ReadingQuranChallenge resultChallenge = response.getData();

    assertThat(groupRepo.count(), is(groupsNumBefore + 1));

    assertThat(resultChallenge.getGroupId(), notNullValue());
    assertThat(resultChallenge.getSubChallenges().size(), greaterThan(0));
    SubChallenge resultFirstSubChallenge = resultChallenge.getSubChallenges().get(0);
    SubChallenge expectedFirstSubChallenge = challenge.getSubChallenges().get(0);
    assertThat(resultFirstSubChallenge.getSurahName(),
        equalTo(expectedFirstSubChallenge.getSurahName()));
    assertThat(resultFirstSubChallenge.getStartingVerseNumber(),
        equalTo(expectedFirstSubChallenge.getStartingVerseNumber()));
    assertThat(resultFirstSubChallenge.getEndingVerseNumber(),
        equalTo(expectedFirstSubChallenge.getEndingVerseNumber()));

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

    assertThat(updatedUser1.getReadingQuranChallenges().size(), is(1));
    assertThat(updatedUser2.getReadingQuranChallenges().size(), is(1));
    assertThat(updatedUser3.getReadingQuranChallenges().size(), is(1));

    assertThat(updatedUser1.getReadingQuranChallenges().get(0).getGroupId(),
        equalTo(user1AddedGroup.getGroupId()));
    assertThat(updatedUser2.getReadingQuranChallenges().get(0).getGroupId(),
        equalTo(user1AddedGroup.getGroupId()));
    assertThat(updatedUser3.getReadingQuranChallenges().get(0).getGroupId(),
        equalTo(user1AddedGroup.getGroupId()));

    assertThat(
        updatedUser1.getReadingQuranChallenges().get(0).getSubChallenges().get(0).getSurahName(),
        equalTo(expectedFirstSubChallenge.getSurahName()));

    Group updatedGroup = groupRepo.findById(user1AddedGroup.getGroupId()).get();
    assertThat(updatedGroup.getUsersIds().size(), is(3));
    assertThat(updatedGroup.getUsersIds(),
        containsInAnyOrder(user1.getId(), user2.getId(), user3.getId()));
  }

  @Test
  public void addReadingQuranChallenge_notFriend_shouldFail() throws Exception {
    User user2 = getNewRegisteredUser();
    User user3 = getNewRegisteredUser();

    azkarApi.makeFriends(user1, user2);

    int user1GroupsNumBefore = userRepo.findById(user1.getId()).get().getUserGroups().size();
    int user2GroupsNumBefore = userRepo.findById(user2.getId()).get().getUserGroups().size();
    int user3GroupsNumBefore = userRepo.findById(user3.getId()).get().getUserGroups().size();
    long groupsNumBefore = groupRepo.count();

    List<String> friendsIds = ImmutableList.of(user2.getId(), user3.getId());
    ReadingQuranChallenge challenge =
        ChallengeFactory.getNewReadingChallenge("toBeRemovedGroupId").toBuilder()
            .groupId(null)
            .build();

    AddReadingQuranChallengeRequest request =
        AddReadingQuranChallengeRequest.AddReadingQuranChallengeRequestBuilder().
            friendsIds(friendsIds)
            .challenge(challenge)
            .build();
    AddReadingQuranChallengeResponse expectedResponse = new AddReadingQuranChallengeResponse();
    expectedResponse.setStatus(new Status(Status.ONE_OR_MORE_USERS_NOT_FRIENDS_ERROR));
    azkarApi.addReadingQuranChallenge(user1, request)
        .andExpect(status().isBadRequest())
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    assertThat(groupRepo.count(), is(groupsNumBefore));

    User updatedUser1 = userRepo.findById(user1.getId()).get();
    User updatedUser2 = userRepo.findById(user2.getId()).get();
    User updatedUser3 = userRepo.findById(user3.getId()).get();
    assertThat(updatedUser1.getUserGroups().size(), is(user1GroupsNumBefore));
    assertThat(updatedUser2.getUserGroups().size(), is(user2GroupsNumBefore));
    assertThat(updatedUser3.getUserGroups().size(), is(user3GroupsNumBefore));

    assertThat(updatedUser1.getReadingQuranChallenges().size(), is(0));
    assertThat(updatedUser2.getReadingQuranChallenges().size(), is(0));
    assertThat(updatedUser3.getReadingQuranChallenges().size(), is(0));
  }

  @Test
  public void addReadingQuranChallenge_oneFriendProvided_shouldFail() throws Exception {
    User user2 = getNewRegisteredUser();

    azkarApi.makeFriends(user1, user2);

    int user1GroupsNumBefore = userRepo.findById(user1.getId()).get().getUserGroups().size();
    int user2GroupsNumBefore = userRepo.findById(user2.getId()).get().getUserGroups().size();
    long groupsNumBefore = groupRepo.count();

    List<String> friendsIds = ImmutableList.of(user2.getId());
    ReadingQuranChallenge challenge =
        ChallengeFactory.getNewReadingChallenge("toBeRemovedGroupId").toBuilder()
            .groupId(null)
            .build();

    AddReadingQuranChallengeRequest request =
        AddReadingQuranChallengeRequest.AddReadingQuranChallengeRequestBuilder().
            friendsIds(friendsIds)
            .challenge(challenge)
            .build();
    AddReadingQuranChallengeResponse expectedResponse = new AddReadingQuranChallengeResponse();
    expectedResponse.setStatus(new Status(Status.LESS_THAN_TWO_FRIENDS_ARE_PROVIDED_ERROR));
    azkarApi.addReadingQuranChallenge(user1, request)
        .andExpect(status().isBadRequest())
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    assertThat(groupRepo.count(), is(groupsNumBefore));

    User updatedUser1 = userRepo.findById(user1.getId()).get();
    User updatedUser2 = userRepo.findById(user2.getId()).get();
    assertThat(updatedUser1.getUserGroups().size(), is(user1GroupsNumBefore));
    assertThat(updatedUser2.getUserGroups().size(), is(user2GroupsNumBefore));

    assertThat(updatedUser1.getReadingQuranChallenges().size(), is(0));
    assertThat(updatedUser2.getReadingQuranChallenges().size(), is(0));
  }

  @Test
  public void addReadingQuranChallenge_duplicateFriendIds_shouldFail() throws Exception {
    User user2 = getNewRegisteredUser();

    azkarApi.makeFriends(user1, user2);

    int user1GroupsNumBefore = userRepo.findById(user1.getId()).get().getUserGroups().size();
    int user2GroupsNumBefore = userRepo.findById(user2.getId()).get().getUserGroups().size();
    long groupsNumBefore = groupRepo.count();

    List<String> friendsIds = ImmutableList.of(user2.getId(), user2.getId());
    ReadingQuranChallenge challenge =
        ChallengeFactory.getNewReadingChallenge("toBeRemovedGroupId").toBuilder()
            .groupId(null)
            .build();

    AddReadingQuranChallengeRequest request =
        AddReadingQuranChallengeRequest.AddReadingQuranChallengeRequestBuilder().
            friendsIds(friendsIds)
            .challenge(challenge)
            .build();
    AddReadingQuranChallengeResponse expectedResponse = new AddReadingQuranChallengeResponse();
    expectedResponse.setStatus(new Status(Status.DUPLICATE_FRIEND_IDS_PROVIDED_ERROR));
    azkarApi.addReadingQuranChallenge(user1, request)
        .andExpect(status().isBadRequest())
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    assertThat(groupRepo.count(), is(groupsNumBefore));

    User updatedUser1 = userRepo.findById(user1.getId()).get();
    User updatedUser2 = userRepo.findById(user2.getId()).get();
    assertThat(updatedUser1.getUserGroups().size(), is(user1GroupsNumBefore));
    assertThat(updatedUser2.getUserGroups().size(), is(user2GroupsNumBefore));

    assertThat(updatedUser1.getReadingQuranChallenges().size(), is(0));
    assertThat(updatedUser2.getReadingQuranChallenges().size(), is(0));
  }

  @Test
  public void addReadingQuranChallenge_pastExpiryDate_shouldNotSucceed() throws Exception {
    User user2 = getNewRegisteredUser();

    azkarApi.makeFriends(user1, user2);

    long pastExpiryDate = Instant.now().getEpochSecond() - ChallengeFactory.EXPIRY_DATE_OFFSET;
    ReadingQuranChallenge challenge = ReadingQuranChallenge.builder()
        .expiryDate(pastExpiryDate)
        .subChallenges(
            ImmutableList.of(ChallengeFactory.quranSubChallenge1()))
        .groupId(validGroup.getId())
        .build();
    AddReadingQuranChallengeResponse expectedResponse = new AddReadingQuranChallengeResponse();
    expectedResponse.setStatus(new Status(Status.PAST_EXPIRY_DATE_ERROR));

    AddReadingQuranChallengeRequest request =
        AddReadingQuranChallengeRequest.AddReadingQuranChallengeRequestBuilder().
            friendsIds(ImmutableList.of(user2.getId()))
            .challenge(challenge)
            .build();
    azkarApi.addReadingQuranChallenge(user1, request)
        .andExpect(status().isBadRequest())
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    List<ReadingQuranChallenge> challengesProgress = userRepo.findById(user1.getId()).get()
        .getReadingQuranChallenges();
    assertTrue("UserChallenges list is not empty.", challengesProgress.isEmpty());
    List<String> groupChallenges = groupRepo.findById(validGroup.getId()).get().getChallengesIds();
    assertTrue("GroupChallenges list is expected to be empty but it is not.",
        groupChallenges.isEmpty());
  }

  @Test
  public void addReadingQuranChallenge_duplicateSurah_shouldFail() throws Exception {
    User user2 = getNewRegisteredUser();

    azkarApi.makeFriends(user1, user2);

    ReadingQuranChallenge challenge =
        ChallengeFactory.getNewReadingChallenge("toBeRemovedGroupId").toBuilder()
            .groupId(null)
            .build();

    // Duplicate subChallenge
    challenge.setSubChallenges(
        ImmutableList.of(
            SubChallenge.builder()
                .surahName("surahName")
                .startingVerseNumber(2)
                .endingVerseNumber(4)
                .build(), SubChallenge.builder()
                .surahName("surahName")
                .startingVerseNumber(2)
                .endingVerseNumber(3)
                .build()));
    AddReadingQuranChallengeRequest request =
        AddReadingQuranChallengeRequest.AddReadingQuranChallengeRequestBuilder().
            friendsIds(ImmutableList.of(user2.getId()))
            .challenge(challenge)
            .build();

    AddReadingQuranChallengeResponse expectedResponse = new AddReadingQuranChallengeResponse();
    expectedResponse.setStatus(new Status(Status.CHALLENGE_CREATION_DUPLICATE_SURAH_NAME_ERROR));
    azkarApi.addReadingQuranChallenge(user1, request)
        .andExpect(status().isBadRequest())
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void addReadingQuranChallenge_inconsistentStartAndEndVerseNumbers_shouldFail()
      throws Exception {
    User user2 = getNewRegisteredUser();

    azkarApi.makeFriends(user1, user2);

    ReadingQuranChallenge challenge =
        ChallengeFactory.getNewReadingChallenge("toBeRemovedGroupId").toBuilder()
            .groupId(null)
            .build();

    challenge.setSubChallenges(
        ImmutableList.of(SubChallenge.builder()
            .surahName("surahName")
            .startingVerseNumber(3)
            .endingVerseNumber(2)
            .build()));

    AddReadingQuranChallengeRequest request =
        AddReadingQuranChallengeRequest.AddReadingQuranChallengeRequestBuilder().
            friendsIds(ImmutableList.of(user2.getId()))
            .challenge(challenge)
            .build();

    AddReadingQuranChallengeResponse expectedResponse = new AddReadingQuranChallengeResponse();
    expectedResponse.setStatus(new Status(Status.STARTING_VERSE_AFTER_ENDING_VERSE_ERROR));
    azkarApi.addReadingQuranChallenge(user1, request)
        .andExpect(status().isBadRequest())
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }
}
