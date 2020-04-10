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
import com.azkar.requestbodies.RequestBodyException;
import com.azkar.requestbodies.usercontroller.AddUserRequestBody;
import com.azkar.requestbodies.usercontroller.ResolveFriendRequestBody;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
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

  @GetMapping(path = "/users/{id}")
  public ResponseEntity<GetUserResponse> getUser(@PathVariable String id) {
    Optional<User> user = userRepo.findById(id);
    if (!user.isPresent()) {
      return ResponseEntity.notFound().build();
    }
    GetUserResponse response = new GetUserResponse();
    response.setData(user.get());
    return ResponseEntity.ok(response);
  }

  @PostMapping(path = "/user", consumes = JSON_CONTENT_TYPE, produces = JSON_CONTENT_TYPE)
  public ResponseEntity<AddUserResponse> addUser(@RequestBody User user) {
    User newUser = User.builder().name(user.getName()).email(user.getEmail()).build();
    userRepo.save(newUser);
    AddUserResponse response = new AddUserResponse();
    response.setData(newUser);
    return ResponseEntity.ok(response);
  }

  @GetMapping(path = "/users/friends", produces = JSON_CONTENT_TYPE)
  public ResponseEntity<GetFriendsResponse> getFriends() {
    GetFriendsResponse response = new GetFriendsResponse();

    List<Friendship> friendships = friendshipRepo.findByRequesterId(getCurrentUser().getId());
    friendships.addAll(friendshipRepo.findByResponderId(getCurrentUser().getId()));
    response.setData(friendships);
    return ResponseEntity.ok(response);
  }

  @PostMapping(path = "/users/friends/{id}", produces =)
  public ResponseEntity<AddFriendResponse> addFriend(@PathVariable String id) {
    AddFriendResponse response = new AddFriendResponse();

    // Check if the provided id is valid.
    Optional<User> responder = userRepo.findById(id);
    if (!responder.isPresent()) {
      response.setError(new Error(AddFriendResponse.kUserNotFoundError));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    // Check if the current user already requested friendship with the other user.
    Optional<Friendship> friendship = friendshipRepo
        .findByRequesterIdAndResponderId(getCurrentUser().getId(), responder.get().getId());
    if (friendship.isPresent()) {
      response.setError(new Error(AddFriendResponse.kFriendshipAlreadyRequestedError));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    // Check if the other user already requested friendship with the current user. If this is the
    // case then the friendship should not be pending anymore.
    friendship = friendshipRepo.findByRequesterIdAndResponderId(
        responder.get().getId(),
        getCurrentUser().getId());
    if (friendship.isPresent()) {
      friendshipRepo.save(
          Friendship.builder()
              .requesterId(responder.get().getId())
              .responderId(getCurrentUser().getId())
              .isPending(false)
              .build());
      return ResponseEntity.ok().body(response);
    }

    friendshipRepo.insert(
        Friendship.builder()
            .requesterId(getCurrentUser().getId())
            .requesterUsername(getCurrentUser().getUsername())
            .responderId(responder.get().getId())
            .responderUsername(responder.get().getUsername())
            .isPending(true)
            .build());
    return ResponseEntity.ok(response);
  }

  @PutMapping(path = "users/friends/{id}")
  public ResponseEntity<ResolveFriendRequestResponse> resolveFriendRequest(
      @PathVariable String id, @RequestBody ResolveFriendRequestBody resolveFriendRequestBody) {
    resolveFriendRequestBody.validate();
    ResolveFriendRequestResponse response = new ResolveFriendRequestResponse();

    // Check if there is a friendship relation pending for the two users where the current user
    // was the responder.
    Optional<Friendship> friendship =
        friendshipRepo.findByRequesterIdAndResponderId(id, getCurrentUser().getId());
    if (!friendship.isPresent()) {
      response.setError(new Error(ResolveFriendRequestResponse.kFriendshipNotFoundError));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    // Check if the users are already friends.
    if (!friendship.get().isPending()) {
      response.setError(new Error(ResolveFriendRequestResponse.kFriendshipNotPendingError));
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
        friendshipRepo.findByRequesterIdAndResponderId(getCurrentUser().getId(), id);
    if (!friendship.isPresent()) {
      friendship = friendshipRepo.findByRequesterIdAndResponderId(id, getCurrentUser().getId());
    }

    if (!friendship.isPresent()) {
      return ResponseEntity.unprocessableEntity().body(response);
    }

    return ResponseEntity.ok(response);
  }
}
