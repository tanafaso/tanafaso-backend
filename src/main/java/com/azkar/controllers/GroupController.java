package com.azkar.controllers;

import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.entities.User.UserGroup;
import com.azkar.payload.ResponseBase.Status;
import com.azkar.payload.groupcontroller.requests.AddGroupRequest;
import com.azkar.payload.groupcontroller.responses.AddGroupResponse;
import com.azkar.payload.groupcontroller.responses.AddToGroupResponse;
import com.azkar.payload.groupcontroller.responses.GetGroupLeaderboardResponse;
import com.azkar.payload.groupcontroller.responses.GetGroupResponse;
import com.azkar.payload.groupcontroller.responses.GetGroupsResponse;
import com.azkar.payload.groupcontroller.responses.GetUserGroupsResponse;
import com.azkar.payload.groupcontroller.responses.LeaveGroupResponse;
import com.azkar.payload.utils.UserScore;
import com.azkar.repos.ChallengeRepo;
import com.azkar.repos.FriendshipRepo;
import com.azkar.repos.GroupRepo;
import com.azkar.repos.UserRepo;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/groups", produces = MediaType.APPLICATION_JSON_VALUE)
public class GroupController extends BaseController {

  private static final Logger logger = LoggerFactory.getLogger(GroupController.class);

  @Autowired
  private GroupRepo groupRepo;

  @Autowired
  private ChallengeRepo challengeRepo;

  @Autowired
  private UserRepo userRepo;

  @Autowired
  private FriendshipRepo friendshipRepo;

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<AddGroupResponse> addGroup(@RequestBody AddGroupRequest req) {
    req.validate();

    User currentUser = getCurrentUser(userRepo);
    Group newGroup =
        Group.builder()
            .name(req.getName())
            .creatorId(currentUser.getId())
            .usersIds(new ArrayList<>(Arrays.asList(currentUser.getId())))
            .build();
    newGroup = groupRepo.save(newGroup);

    currentUser.getUserGroups()
        .add(UserGroup.builder().groupId(newGroup.getId()).groupName(newGroup.getName())
            .invitingUserId(currentUser.getId())
            .build());
    userRepo.save(currentUser);

    AddGroupResponse response = new AddGroupResponse();
    response.setData(newGroup);
    return ResponseEntity.ok(response);
  }

  @GetMapping(value = "/{groupId}")
  public ResponseEntity<GetGroupResponse> getGroup(@PathVariable String groupId) {
    GetGroupResponse response = new GetGroupResponse();
    User currentUser = getCurrentUser(userRepo);
    if (!currentUser.getUserGroups().stream().anyMatch(
        userGroup ->
            userGroup.getGroupId().equals(groupId)
    )) {
      response.setStatus(new Status(Status.NOT_MEMBER_IN_GROUP_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    Optional<Group> group = groupRepo.findById(groupId);
    // Check whether the group is deleted.
    if (!group.isPresent()) {
      response.setStatus(new Status(Status.NOT_MEMBER_IN_GROUP_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    response.setData(group.get());
    return ResponseEntity.ok(response);
  }

  @GetMapping("/userGroups")
  public ResponseEntity<GetUserGroupsResponse> getUserGroups() {
    GetUserGroupsResponse response = new GetUserGroupsResponse();

    User user = userRepo.findById(getCurrentUser().getUserId()).get();
    response.setData(user.getUserGroups());

    return ResponseEntity.ok(response);
  }

  @GetMapping()
  public ResponseEntity<GetGroupsResponse> getGroups() {
    GetGroupsResponse response = new GetGroupsResponse();

    response.setData(groupRepo.findAll().stream()
        .filter(group -> group.getUsersIds().contains(getCurrentUser().getUserId()))
        .collect(Collectors.toList()));

    return ResponseEntity.ok(response);
  }

  @GetMapping(value = "/{groupId}/leaderboard")
  public ResponseEntity<GetGroupLeaderboardResponse> getGroupLeaderboard(
      @PathVariable String groupId) {
    GetGroupLeaderboardResponse response = new GetGroupLeaderboardResponse();
    User currentUser = getCurrentUser(userRepo);
    if (!currentUser.getUserGroups().stream().anyMatch(
        userGroup ->
            userGroup.getGroupId().equals(groupId)
    )) {
      response.setStatus(new Status(Status.NOT_MEMBER_IN_GROUP_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    Optional<Group> group = groupRepo.findById(groupId);
    if (!group.isPresent()) {
      throw new RuntimeException(String.format("User with id: %s, trying to get leaderboard of a "
          + "non-existing group", currentUser.getId()));
    }

    boolean isBinaryGroup =
        friendshipRepo.findByUserId(currentUser.getId()).getFriends().stream().anyMatch(
            friend -> friend.getGroupId().equals(groupId)
        );
    if (group.get().getUsersIds().size() != 2) {
      logger.error("Found conflicting information regarding a binary group with ID %s that has "
          + "more than two members", group.get().getId());
      // Fallback to calculating the leaderboard for non-binary groups.
      isBinaryGroup = false;
    }

    List<UserScore> userScores;
    if (isBinaryGroup) {
      userScores = getBinaryGroupLeaderboard(group.get());
    } else {
      userScores = getNonBinaryGroupLeaderboard(group.get());
    }
    response.setData(userScores);
    return ResponseEntity.ok(response);
  }

  // Accumulates the scores of the two users in all of the groups they are both members in.
  private List<UserScore> getBinaryGroupLeaderboard(Group group) {
    AtomicInteger user1Score = new AtomicInteger(0);
    AtomicInteger user2Score = new AtomicInteger(0);
    User user1 = userRepo.findById(group.getUsersIds().get(0)).get();
    User user2 = userRepo.findById(group.getUsersIds().get(1)).get();
    groupRepo.findAll().stream().filter(
        grp -> (grp.getUsersIds().contains(user1.getId()) && grp.getUsersIds()
            .contains(user2.getId())))
        .forEach(grp -> {
          Optional<UserScore> user1ScoreInGrp = getUserScoreInGroup(user1.getId(), grp);
          Optional<UserScore> user2ScoreInGrp = getUserScoreInGroup(user2.getId(), grp);
          user1ScoreInGrp.ifPresent(userScore -> user1Score.addAndGet(userScore.getTotalScore()));
          user2ScoreInGrp.ifPresent(userScore -> user2Score.addAndGet(userScore.getTotalScore()));
        });
    UserScore userScore1 = UserScore.builder()
        .username(user1.getUsername())
        .firstName(user1.getFirstName())
        .lastName(user1.getLastName())
        .totalScore(user1Score.get())
        .build();
    UserScore userScore2 = UserScore.builder()
        .username(user2.getUsername())
        .firstName(user2.getFirstName())
        .lastName(user2.getLastName())
        .totalScore(user2Score.get())
        .build();
    return ImmutableList.of(userScore1, userScore2);
  }

  private List<UserScore> getNonBinaryGroupLeaderboard(Group group) {
    List<UserScore> userScores = new ArrayList<>();
    for (String groupMemberId : group.getUsersIds()) {
      Optional<UserScore> userScore = getUserScoreInGroup(groupMemberId, group);
      if (!userScore.isPresent()) {
        logger.warn(String.format("Dangling group member: %s in group: %s", groupMemberId,
            group.getId()));
        continue;
      }
      userScores.add(userScore.get());
    }

    Collections.sort(userScores,
        (u1, u2) -> Integer.compare(u2.getTotalScore(), u1.getTotalScore()));

    return userScores;
  }

  private Optional<UserScore> getUserScoreInGroup(String userId, Group group) {
    Optional<User> user = userRepo.findById(userId);
    if (!user.isPresent()) {
      return Optional.empty();
    }

    Optional<UserGroup> userGroup =
        user.get().getUserGroups().stream()
            .filter(userGroup1 -> userGroup1.getGroupId().equals(group.getId())).findFirst();
    if (!userGroup.isPresent()) {
      return Optional.empty();
    }

    return Optional.of(
        UserScore.builder().firstName(user.get().getFirstName()).lastName(user.get().getLastName())
            .username(user.get().getUsername())
            .totalScore(userGroup.get().getTotalScore()).build());
  }

  @PutMapping(value = "/{groupId}/add/{userId}")
  public ResponseEntity<AddToGroupResponse> addUser(
      @PathVariable String groupId,
      @PathVariable(value = "userId") String invitedUserId) {
    AddToGroupResponse response = new AddToGroupResponse();

    // Check if the ID of the user to be added is valid.
    Optional<User> userToAdd = userRepo.findById(invitedUserId);
    if (!userToAdd.isPresent()) {
      response.setStatus(new Status(Status.INVITED_USER_INVALID_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    // Check if the group id is valid.
    Optional<Group> group = groupRepo.findById(groupId);
    if (!group.isPresent()) {
      response.setStatus(new Status(Status.GROUP_INVALID_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    // Check if the two users are friends
    if (!friendshipRepo.findByUserId(getCurrentUser().getUserId()).getFriends().stream()
        .anyMatch(friend -> friend.getUserId().equals(invitedUserId) && !friend.isPending())) {
      response.setStatus(new Status(Status.NO_FRIENDSHIP_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    // Check if the inviting user is a member of the group.
    if (!isMember(userRepo.findById(getCurrentUser().getUserId()).get(), group.get())) {
      response.setStatus(new Status(Status.INVITING_USER_IS_NOT_MEMBER_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    // Check if the invited user is already a member of the group.
    if (isMember(userToAdd.get(), group.get())) {
      response.setStatus(new Status(Status.INVITED_USER_ALREADY_MEMBER_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    User invitingUser = userRepo.findById(getCurrentUser().getUserId()).get();
    // Only add the user group but don't add old challenges.
    userToAdd.get().getUserGroups().add(
        UserGroup.builder()
            .groupId(groupId)
            .groupName(group.get().getName())
            .invitingUserId(invitingUser.getId())
            .build());
    group.get().getUsersIds().add(userToAdd.get().getId());
    userRepo.save(userToAdd.get());
    groupRepo.save(group.get());
    return ResponseEntity.ok(response);
  }

  @PutMapping(value = "/{groupId}/leave")
  public ResponseEntity<LeaveGroupResponse> leave(@PathVariable String groupId) {
    LeaveGroupResponse response = new LeaveGroupResponse();

    User user = userRepo.findById(getCurrentUser().getUserId()).get();
    Optional<Group> group = groupRepo.findById(groupId);

    // Check if the group id is valid.
    if (!group.isPresent()) {
      response.setStatus(new Status(Status.GROUP_INVALID_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    // Check if the user is not already a member of the group.
    if (!isMember(user, group.get())) {
      response.setStatus(new Status(Status.NOT_MEMBER_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    group.get().getUsersIds().removeIf(userId -> userId.equals(user.getId()));

    List<UserGroup> userGroups = user.getUserGroups();
    userGroups.removeIf(userGroup -> userGroup.getGroupId().equals(groupId));

    userRepo.save(user);
    groupRepo.save(group.get());
    return ResponseEntity.ok(response);
  }

  private boolean isMember(User user, Group group) {
    return group.getUsersIds().stream().anyMatch(userId -> userId.equals(user.getId()));
  }

}
