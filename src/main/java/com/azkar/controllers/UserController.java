package com.azkar.controllers;

import com.azkar.entities.Friendship;
import com.azkar.entities.User;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.usercontroller.AddFriendResponse;
import com.azkar.payload.usercontroller.AddUserResponse;
import com.azkar.payload.usercontroller.DeleteFriendResponse;
import com.azkar.payload.usercontroller.GetFriendsResponse;
import com.azkar.payload.usercontroller.GetUserResponse;
import com.azkar.payload.usercontroller.GetUsersResponse;
import com.azkar.payload.usercontroller.ResolveFriendRequestResponse;
import com.azkar.repos.FriendshipRepo;
import com.azkar.repos.UserRepo;
import com.azkar.requestbodies.usercontroller.ResolveFriendRequestBody;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController extends BaseController {

  @Autowired
  private UserRepo userRepo;
  @Autowired
  private FriendshipRepo friendshipRepo;

  @GetMapping(path = "/users")
  public ResponseEntity<GetUsersResponse> getUsers() {
    GetUsersResponse response = new GetUsersResponse();
    response.setData(userRepo.findAll());
    return ResponseEntity.ok(response);
  }

  @GetMapping(path = "/user/{id}")
  public ResponseEntity<GetUserResponse> getUser(@PathVariable String id) {
    Optional<User> user = userRepo.findById(id);
    if (!user.isPresent()) {
      return ResponseEntity.notFound().build();
    }
    GetUserResponse response = new GetUserResponse();
    response.setData(user.get());
    return ResponseEntity.ok(response);
  }

  @PostMapping(path = "/user", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<AddUserResponse> addUser(@RequestBody User user) {
    User newUser = User.builder().name(user.getName()).email(user.getEmail()).build();
    userRepo.save(newUser);
    AddUserResponse response = new AddUserResponse();
    response.setData(newUser);
    return ResponseEntity.ok(response);
  }

  @GetMapping(path = "/users/friends")
  public ResponseEntity<GetFriendsResponse> getFriends() {
    GetFriendsResponse response = new GetFriendsResponse();

    List<Friendship> friendships = friendshipRepo.findByRequesterId(getCurrentUser().getUserId());
    friendships.addAll(friendshipRepo.findByResponderId(getCurrentUser().getUserId()));
    response.setData(friendships);
    return ResponseEntity.ok(response);
  }

  @PostMapping(path = "/users/friends/{id}")
  public ResponseEntity<AddFriendResponse> addFriend(@PathVariable String id) {
    AddFriendResponse response = new AddFriendResponse();

    // Check if the provided id is valid.
    Optional<User> responder = userRepo.findById(id);
    if (!responder.isPresent()) {
      response.setError(new Error(AddFriendResponse.ERROR_USER_NOT_FOUND));
      return ResponseEntity.badRequest().body(response);
    }

    // Check if the current user already requested friendship with the other user.
    Optional<Friendship> friendship = friendshipRepo
        .findByRequesterIdAndResponderId(getCurrentUser().getUserId(), responder.get().getId());
    if (friendship.isPresent()) {
      response.setError(new Error(AddFriendResponse.ERROR_FRIENDSHIP_ALREADY_REQUESTED));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    // Check if the other user already requested friendship with the current user. If this is the
    // case then the friendship should not be pending anymore.
    friendship = friendshipRepo.findByRequesterIdAndResponderId(
        responder.get().getId(),
        getCurrentUser().getUserId());
    if (friendship.isPresent()) {
      friendship.get().setPending(false);
      friendshipRepo.save(friendship.get());
      return ResponseEntity.ok().body(response);
    }

    friendshipRepo.insert(
        Friendship.builder()
            .requesterId(getCurrentUser().getUserId())
            .requesterUsername(getCurrentUser().getUsername())
            .responderId(responder.get().getId())
            .responderUsername(responder.get().getUsername())
            .isPending(true)
            .build());
    return ResponseEntity.ok(response);
  }

  @PutMapping(path = "users/friends/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResolveFriendRequestResponse> resolveFriendRequest(
      @PathVariable String id, @RequestBody ResolveFriendRequestBody resolveFriendRequestBody) {
    ResolveFriendRequestResponse response = new ResolveFriendRequestResponse();
    if (!resolveFriendRequestBody.validate()) {
      response.setError(new Error("Unexpected request body"));
      return ResponseEntity.badRequest().body(response);
    }

    // Check if there is a friendship relation pending for the two users where the current user
    // is the responder.
    Optional<Friendship> friendship =
        friendshipRepo.findByRequesterIdAndResponderId(id, getCurrentUser().getUserId());
    if (!friendship.isPresent()) {
      response.setError(new Error(ResolveFriendRequestResponse.ERROR_NO_FRIEND_REQUEST_EXIST));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    // Check if the users are already friends.
    if (!friendship.get().isPending()) {
      response
          .setError(new Error(ResolveFriendRequestResponse.ERROR_FRIEND_REQUEST_ALREADY_ACCEPTED));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    if (resolveFriendRequestBody.isAccept()) {
      friendship.get().setPending(false);
      friendshipRepo.save(friendship.get());
    } else {
      friendshipRepo.delete(friendship.get());
    }
    return ResponseEntity.ok(response);
  }

  @DeleteMapping(path = "users/friends/{id}")
  public ResponseEntity<DeleteFriendResponse> deleteFriend(@PathVariable String id) {
    DeleteFriendResponse response = new DeleteFriendResponse();

    Optional<Friendship> friendship =
        friendshipRepo.findByRequesterIdAndResponderId(getCurrentUser().getUserId(), id);
    if (!friendship.isPresent()) {
      friendship = friendshipRepo.findByRequesterIdAndResponderId(id, getCurrentUser().getUserId());
    }

    if (!friendship.isPresent()) {
      response.setError(new Error(DeleteFriendResponse.ERROR_NO_FRIENDSHIP));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    friendshipRepo.delete(friendship.get());
    return ResponseEntity.ok(response);
  }
}
