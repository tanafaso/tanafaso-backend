package com.azkar.controllers;

import com.azkar.entities.Friendship;
import com.azkar.entities.Friendship.Friend;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.entities.User.UserGroup;
import com.azkar.payload.ResponseBase.Status;
import com.azkar.payload.usercontroller.responses.AddFriendResponse;
import com.azkar.payload.usercontroller.responses.DeleteFriendResponse;
import com.azkar.payload.usercontroller.responses.GetFriendsLeaderboardResponse;
import com.azkar.payload.usercontroller.responses.GetFriendsLeaderboardResponse.FriendshipScores;
import com.azkar.payload.usercontroller.responses.GetFriendsResponse;
import com.azkar.payload.usercontroller.responses.ResolveFriendRequestResponse;
import com.azkar.payload.utils.UserScore;
import com.azkar.repos.FriendshipRepo;
import com.azkar.repos.GroupRepo;
import com.azkar.repos.UserRepo;
import com.azkar.services.NotificationsService;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/friends", produces = MediaType.APPLICATION_JSON_VALUE)
public class FriendshipController extends BaseController {

  @Autowired
  NotificationsService notificationsService;

  @Autowired
  FriendshipRepo friendshipRepo;

  @Autowired
  UserRepo userRepo;

  @Autowired
  GroupRepo groupRepo;

  @GetMapping
  public ResponseEntity<GetFriendsResponse> getFriends() {
    GetFriendsResponse response = new GetFriendsResponse();

    Friendship friendship = friendshipRepo.findByUserId(getCurrentUser().getUserId());
    response.setData(friendship);
    return ResponseEntity.ok(response);
  }

  @GetMapping(path = "/leaderboard")
  public ResponseEntity<GetFriendsLeaderboardResponse> getFriendsLeaderboard() {
    GetFriendsLeaderboardResponse response = new GetFriendsLeaderboardResponse();

    User currentUser = getCurrentUser(userRepo);

    List<FriendshipScores> friendsScores = new ArrayList<>();
    Friendship friendship = friendshipRepo.findByUserId(getCurrentUser().getUserId());
    friendship.getFriends().stream().forEach(friend -> {
      if (friend.isPending()) {
        return;
      }

      Optional<User> friendUser = userRepo.findById(friend.getUserId());
      if (!friendUser.isPresent()) {
        return;
      }

      List<UserScore> scores = getFriendsScores(currentUser, friendUser.get());
      friendsScores.add(FriendshipScores.builder()
          .currentUserScore(scores.get(0))
          .friendScore(scores.get(1))
          .build());
    });

    response.setData(friendsScores);
    return ResponseEntity.ok(response);
  }

  @PutMapping(path = "/{id}")
  public ResponseEntity<AddFriendResponse> add(
      @PathVariable(value = "id") String otherUserId) {
    AddFriendResponse response = new AddFriendResponse();

    User currentUser = userRepo.findById(getCurrentUser().getUserId()).get();
    if (currentUser.getId().equals(otherUserId)) {
      response.setStatus(new Status(Status.ADD_SELF_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    // Check if the provided id is valid.
    Optional<User> otherUser = userRepo.findById(otherUserId);
    if (!otherUser.isPresent()) {
      response.setStatus(new Status(Status.USER_NOT_FOUND_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    // Check if the current user already requested friendship with the other user.
    Friendship otherUserFriendship = friendshipRepo.findByUserId(otherUserId);
    if (otherUserFriendship.getFriends().stream()
        .anyMatch(friend -> friend.getUserId().equals(currentUser.getId()))) {
      response.setStatus(new Status(Status.FRIENDSHIP_ALREADY_REQUESTED_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    // Check if the other user already requested friendship with the current user. If this is the
    // case then the friendship should not be pending anymore.
    Friendship currentUserFriendship = friendshipRepo.findByUserId(currentUser.getId());
    Optional<Friend> friend = currentUserFriendship.getFriends().stream()
        .filter(f -> f.getUserId().equals(otherUserId))
        .findAny();
    if (friend.isPresent()) {
      // Set isPending for the current user.
      friend.get().setPending(false);

      // Set isPending for the current user.
      otherUserFriendship.getFriends().add(
          Friend.builder()
              .userId(currentUser.getId())
              .username(currentUser.getUsername())
              .firstName(currentUser.getFirstName())
              .lastName(currentUser.getLastName())
              .isPending(false)
              .build()
      );

      friendshipRepo.save(currentUserFriendship);
      friendshipRepo.save(otherUserFriendship);
      return ResponseEntity.ok().body(response);
    }

    otherUserFriendship.getFriends().add(
        Friend.builder()
            .userId(currentUser.getId())
            .username(currentUser.getUsername())
            .firstName(currentUser.getFirstName())
            .lastName(currentUser.getLastName())
            .isPending(true)
            .build()
    );
    friendshipRepo.save(otherUserFriendship);
    notificationsService.sendNotificationToUser(otherUser.get(), "لديك طلب صداقة جديد",
        currentUser.getFirstName() + " " + currentUser.getLastName());

    return ResponseEntity.ok(response);
  }

  @PutMapping(path = "/{id}/accept")
  public ResponseEntity<ResolveFriendRequestResponse> accept(
      @PathVariable(value = "id") String otherUserId) {
    ResolveFriendRequestResponse response = new ResolveFriendRequestResponse();

    User currentUser = userRepo.findById(getCurrentUser().getUserId()).get();

    // Assert that the current user has a pending friend request from the other user.
    Friendship currentUserFriendship = friendshipRepo.findByUserId(currentUser.getId());
    Optional<Friend> friend = currentUserFriendship.getFriends().stream()
        .filter(f -> f.getUserId().equals(otherUserId))
        .findAny();
    if (!friend.isPresent()) {
      response.setStatus(new Status(Status.NO_FRIEND_REQUEST_EXIST_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    // Check if the users are already friends.
    if (!friend.get().isPending()) {
      response
          .setStatus(new Status(Status.FRIEND_REQUEST_ALREADY_ACCEPTED_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    Group binaryGroup = generateBinaryGroup(currentUser, friend.get());
    groupRepo.save(binaryGroup);

    friend.get().setPending(false);
    friend.get().setGroupId(binaryGroup.getId());
    Friendship otherUserFriendship = friendshipRepo.findByUserId(otherUserId);

    otherUserFriendship.getFriends().add(
        Friend.builder()
            .userId(currentUser.getId())
            .username(currentUser.getUsername())
            .firstName(currentUser.getFirstName())
            .lastName(currentUser.getLastName())
            .isPending(false)
            .groupId(binaryGroup.getId())
            .build()
    );

    UserGroup userGroup =
        UserGroup.builder()
            .groupId(binaryGroup.getId())
            .groupName(binaryGroup.getName())
            .invitingUserId(binaryGroup.getCreatorId())
            .monthScore(0)
            .totalScore(0)
            .build();
    currentUser.getUserGroups().add(userGroup);
    User otherUser = userRepo.findById(otherUserId).get();
    otherUser.getUserGroups().add(userGroup);

    friendshipRepo.save(currentUserFriendship);
    friendshipRepo.save(otherUserFriendship);
    userRepo.save(currentUser);
    userRepo.save(otherUser);

    return ResponseEntity.ok(response);
  }

  private Group generateBinaryGroup(User currentUser, Friend friend) {
    // TODO(issue#148): Make Group.adminId a list
    Group group = Group.builder()
        .usersIds(Arrays.asList(currentUser.getId(), friend.getUserId()))
        .creatorId(friend.getUserId())
        .build();
    return group;
  }

  @PutMapping(path = "/{id}/reject")
  public ResponseEntity<ResolveFriendRequestResponse> reject(
      @PathVariable(name = "id") String otherUserId) {
    ResolveFriendRequestResponse response = new ResolveFriendRequestResponse();

    // Assert that the current user has a pending friend request from the other user.
    // Check if the current user already requested friendship with the other user.
    Friendship currentUserFriendship = friendshipRepo.findByUserId(getCurrentUser().getUserId());
    List<Friend> currentUserFriends = currentUserFriendship.getFriends();

    int friendIndex = findFriendIndexInList(otherUserId, currentUserFriends);
    if (friendIndex == -1) {
      response.setStatus(new Status(Status.NO_FRIEND_REQUEST_EXIST_ERROR));
      return ResponseEntity.badRequest().body(response);
    }
    // Check if the users are already friends.
    if (!currentUserFriends.get(friendIndex).isPending()) {
      response
          .setStatus(new Status(Status.FRIEND_REQUEST_ALREADY_ACCEPTED_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    currentUserFriends.remove(friendIndex);
    friendshipRepo.save(currentUserFriendship);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping(path = "/{id}")
  public ResponseEntity<DeleteFriendResponse> deleteFriend(
      @PathVariable(value = "id") String otherUserId) {
    DeleteFriendResponse response = new DeleteFriendResponse();

    // Check if the provided id is valid.
    Optional<User> otherUser = userRepo.findById(otherUserId);
    if (!otherUser.isPresent()) {
      response.setStatus(new Status(Status.USER_NOT_FOUND_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    Friendship currentUserFriendship = friendshipRepo.findByUserId(getCurrentUser().getUserId());
    Friendship otherUserFriendship = friendshipRepo.findByUserId(otherUserId);

    int currentUserAsFriendIndex = findFriendIndexInList(getCurrentUser().getUserId(),
        otherUserFriendship.getFriends());
    int otherUserAsFriendIndex =
        findFriendIndexInList(otherUserId, currentUserFriendship.getFriends());

    if (currentUserAsFriendIndex == -1 || otherUserAsFriendIndex == -1) {
      response.setStatus(new Status(Status.NO_FRIENDSHIP_ERROR));
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    String groupId = currentUserFriendship.getFriends().get(otherUserAsFriendIndex).getGroupId();

    currentUserFriendship.getFriends().remove(otherUserAsFriendIndex);
    otherUserFriendship.getFriends().remove(currentUserAsFriendIndex);

    // Remove Group
    User currentUser = userRepo.findById(getCurrentUser().getUserId()).get();
    currentUser.getUserGroups().removeIf(userGroup -> userGroup.getGroupId().equals(groupId));
    otherUser.get().getUserGroups().removeIf(userGroup -> userGroup.getGroupId().equals(groupId));
    groupRepo.deleteById(groupId);

    userRepo.save(currentUser);
    userRepo.save(otherUser.get());
    friendshipRepo.save(currentUserFriendship);
    friendshipRepo.save(otherUserFriendship);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).body(response);
  }

  private int findFriendIndexInList(String userId, List<Friend> friends) {
    for (int i = 0; i < friends.size(); i++) {
      if (friends.get(i).getUserId().equals(userId)) {
        return i;
      }
    }
    return -1;
  }

  // Accumulates the scores of the two users in all of the groups they are both members in.
  private List<UserScore> getFriendsScores(User user1, User user2) {
    AtomicInteger user1Score = new AtomicInteger(0);
    AtomicInteger user2Score = new AtomicInteger(0);
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
}
