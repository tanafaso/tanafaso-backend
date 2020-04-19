package com.azkar.controllers;

import com.azkar.entities.Friendship;
import com.azkar.entities.Friendship.Friend;
import com.azkar.entities.User;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.usercontroller.AddFriendResponse;
import com.azkar.payload.usercontroller.DeleteFriendResponse;
import com.azkar.payload.usercontroller.GetFriendsResponse;
import com.azkar.payload.usercontroller.ResolveFriendRequestResponse;
import com.azkar.repos.FriendshipRepo;
import com.azkar.repos.UserRepo;
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
  FriendshipRepo friendshipRepo;

  @Autowired
  UserRepo userRepo;

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

    // Check if the provided id is valid.
    Optional<User> otherUser = userRepo.findById(otherUserId);
    if (!otherUser.isPresent()) {
      response.setError(new Error(AddFriendResponse.USER_NOT_FOUND_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    // Check if the current user already requested friendship with the other user.
    Friendship otherUserFriendship = friendshipRepo.findByUserId(otherUserId);
    if (otherUserFriendship.getFriends().stream()
        .anyMatch(friend -> friend.getUserId().equals(getCurrentUser().getUserId()))) {
      response.setError(new Error(AddFriendResponse.FRIENDSHIP_ALREADY_REQUESTED_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    // Check if the other user already requested friendship with the current user. If this is the
    // case then the friendship should not be pending anymore.
    Friendship currentUserFriendship = friendshipRepo.findByUserId(getCurrentUser().getUserId());
    Optional<Friend> friend = currentUserFriendship.getFriends().stream()
        .filter(f -> f.getUserId().equals(otherUserId)).findAny();
    if (friend.isPresent()) {
      // Set isPending for the current user.
      friend.get().setPending(false);

      // Set isPending for the current user.
      otherUserFriendship.getFriends().add(
          Friend.builder()
              .userId(getCurrentUser().getUserId())
              .username(getCurrentUser().getUsername())
              .isPending(false)
              .build()
      );

      friendshipRepo.save(currentUserFriendship);
      friendshipRepo.save(otherUserFriendship);
      return ResponseEntity.ok().body(response);
    }

    otherUserFriendship.getFriends().add(
        Friend.builder()
            .userId(getCurrentUser().getUserId())
            .username(getCurrentUser().getUsername())
            .isPending(true)
            .build()
    );
    friendshipRepo.save(otherUserFriendship);
    return ResponseEntity.ok(response);
  }

  @PutMapping(path = "/{id}/accept")
  public ResponseEntity<ResolveFriendRequestResponse> accept(
      @PathVariable(value = "id") String otherUserId) {
    ResolveFriendRequestResponse response = new ResolveFriendRequestResponse();

    // Assert that the current user has a pending friend request from the other user.
    Friendship currentUserFriendship = friendshipRepo.findByUserId(getCurrentUser().getUserId());
    Optional<Friend> friend = currentUserFriendship.getFriends().stream()
        .filter(f -> f.getUserId().equals(otherUserId)).findAny();
    if (!friend.isPresent()) {
      response.setError(new Error(ResolveFriendRequestResponse.NO_FRIEND_REQUEST_EXIST_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    // Check if the users are already friends.
    if (!friend.get().isPending()) {
      response
          .setError(new Error(ResolveFriendRequestResponse.FRIEND_REQUEST_ALREADY_ACCEPTED_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    friend.get().setPending(false);
    Friendship otherUserFriendship = friendshipRepo.findByUserId(otherUserId);

    otherUserFriendship.getFriends().add(
        Friend.builder()
            .userId(getCurrentUser().getUserId())
            .username(getCurrentUser().getUsername())
            .isPending(false)
            .build()
    );

    friendshipRepo.save(currentUserFriendship);
    friendshipRepo.save(otherUserFriendship);
    return ResponseEntity.ok(response);
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
      response.setError(new Error(ResolveFriendRequestResponse.NO_FRIEND_REQUEST_EXIST_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
    }
    // Check if the users are already friends.
    if (!currentUserFriends.get(friendIndex).isPending()) {
      response
          .setError(new Error(ResolveFriendRequestResponse.FRIEND_REQUEST_ALREADY_ACCEPTED_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
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
      response.setError(new Error(DeleteFriendResponse.USER_NOT_FOUND_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    Friendship currentUserFriendship = friendshipRepo.findByUserId(getCurrentUser().getUserId());
    Friendship otherUserFriendship = friendshipRepo.findByUserId(otherUserId);

    int currentUserAsFriendIndex = findFriendIndexInList(getCurrentUser().getUserId(),
        otherUserFriendship.getFriends());
    int otherUserAsFriendIndex =
        findFriendIndexInList(otherUserId, currentUserFriendship.getFriends());

    if (currentUserAsFriendIndex == -1 || otherUserAsFriendIndex == -1) {
      response.setError(new Error(DeleteFriendResponse.NO_FRIENDSHIP_ERROR));
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    currentUserFriendship.getFriends().remove(otherUserAsFriendIndex);
    otherUserFriendship.getFriends().remove(currentUserAsFriendIndex);

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
