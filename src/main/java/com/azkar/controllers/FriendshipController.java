package com.azkar.controllers;

import com.azkar.entities.Friendship;
import com.azkar.entities.Friendship.Friend;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.entities.User.UserGroup;
import com.azkar.payload.ResponseBase.Status;
import com.azkar.payload.usercontroller.responses.AddFriendResponse;
import com.azkar.payload.usercontroller.responses.DeleteFriendResponse;
import com.azkar.payload.usercontroller.responses.GetFriendsResponse;
import com.azkar.payload.usercontroller.responses.ResolveFriendRequestResponse;
import com.azkar.repos.FriendshipRepo;
import com.azkar.repos.GroupRepo;
import com.azkar.repos.UserRepo;
import com.azkar.services.NotificationsService;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
        .filter(f -> f.getUserId().equals(otherUserId)).findAny();
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
        .filter(f -> f.getUserId().equals(otherUserId)).findAny();
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
        UserGroup.builder().groupId(binaryGroup.getId()).groupName(binaryGroup.getName())
            .invitingUserId(binaryGroup.getAdminId()).isPending(false).monthScore(0).totalScore(0)
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
    String groupName = "binary-generated-" + currentUser.getId() + "-" + friend.getUserId();
    // The adminId should not be used in case of binary groups because both users should have the
    // same capabilities.
    // TODO(issue#148): Make Group.adminId a list
    Group group = Group.builder().name(groupName).usersIds(Arrays.asList(currentUser.getId(),
        friend.getUserId())).adminId(friend.getUserId()).isBinary(true).build();
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
}
