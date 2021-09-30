package com.azkar.controllers;

import com.azkar.entities.PubliclyAvailableFemaleUser;
import com.azkar.entities.PubliclyAvailableMaleUser;
import com.azkar.entities.User;
import com.azkar.payload.ResponseBase.Status;
import com.azkar.payload.usercontroller.requests.SetNotificationTokenRequestBody;
import com.azkar.payload.usercontroller.responses.AddToPubliclyAvailableUsersResponse;
import com.azkar.payload.usercontroller.responses.DeleteFromPubliclyAvailableUsers;
import com.azkar.payload.usercontroller.responses.GetPubliclyAvailableUsersResponse;
import com.azkar.payload.usercontroller.responses.GetPubliclyAvailableUsersResponse.PubliclyAvailableUser;
import com.azkar.payload.usercontroller.responses.GetUserResponse;
import com.azkar.payload.usercontroller.responses.SetNotificationTokenResponse;
import com.azkar.repos.FriendshipRepo;
import com.azkar.repos.PubliclyAvailableFemaleUsersRepo;
import com.azkar.repos.PubliclyAvailableMaleUsersRepo;
import com.azkar.repos.UserRepo;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping(path = "users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController extends BaseController {

  @Autowired
  FriendshipRepo friendshipRepo;
  @Autowired
  private UserRepo userRepo;
  @Autowired
  private PubliclyAvailableMaleUsersRepo publiclyAvailableMaleUsersRepo;
  @Autowired
  private PubliclyAvailableFemaleUsersRepo publiclyAvailableFemaleUsersRepo;

  @GetMapping(path = "/{id}")
  public ResponseEntity<GetUserResponse> getUser(@PathVariable String id) {
    GetUserResponse response = new GetUserResponse();
    /*
//    Return minimal user information, even if not friends so as to allow displaying non-friends in
//    a group.
    if (!friendshipRepo.findByUserId(getCurrentUser(userRepo).getId()).getFriends().stream()
        .anyMatch(friend -> friend.getUserId().equals(id))) {
      response.setStatus(new Status(Status.NO_FRIENDSHIP_ERROR));
      return ResponseEntity.badRequest().body(response);
    }
    */

    Optional<User> user = userRepo.findById(id);
    if (!user.isPresent()) {
      response.setStatus(new Status(Status.USER_NOT_FOUND_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    response.setData(getMinimalInfoAboutUser(user.get()));
    return ResponseEntity.ok(response);
  }

  @GetMapping(path = "/sabeq")
  public ResponseEntity<GetUserResponse> getSabeq() {
    GetUserResponse response = new GetUserResponse();

    Optional<User> user = userRepo.findById(User.SABEQ_ID);
    if (!user.isPresent()) {
      response.setStatus(new Status(Status.USER_NOT_FOUND_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    response.setData(getMinimalInfoAboutUser(user.get()));
    return ResponseEntity.ok(response);
  }

  /**
   * Searches for a user with {@code username} if specified. If {@code username} is not specified,
   * searches for a user with {@code facebook_user_id}.
   */
  // TODO(issue#110): Think if searchForUser return everything about user.
  @GetMapping(path = "/search")
  public ResponseEntity<GetUserResponse> searchForUser(
      @RequestParam(name = "username", required = false) String username,
      @RequestParam(name = "facebook_user_id", required = false) String facebookUserId) {
    if (username != null) {
      return searchForUserByUsername(username);
    }

    if (facebookUserId != null) {
      return searchForUserByFacebookUserId(facebookUserId);
    }

    GetUserResponse response = new GetUserResponse();
    response.setStatus(new Status(Status.SEARCH_PARAMETERS_NOT_SPECIFIED));
    return ResponseEntity.badRequest().body(response);
  }

  @PutMapping(path = "/notifications/token")
  public ResponseEntity<SetNotificationTokenResponse> setNotificationsToken(@RequestBody
      SetNotificationTokenRequestBody body) {
    body.validate();

    User user = getCurrentUser(userRepo);
    user.setNotificationsToken(body.getToken());
    userRepo.save(user);

    return ResponseEntity.ok(new SetNotificationTokenResponse());
  }

  private ResponseEntity<GetUserResponse> searchForUserByUsername(String username) {
    Optional<User> user = userRepo.findByUsername(username);
    GetUserResponse response = new GetUserResponse();
    if (!user.isPresent()) {
      response.setStatus(new Status(Status.USER_NOT_FOUND_ERROR));
      return ResponseEntity.badRequest().body(response);
    }
    response.setData(getMinimalInfoAboutUser(user.get()));
    return ResponseEntity.ok(response);
  }

  private ResponseEntity<GetUserResponse> searchForUserByFacebookUserId(String facebookUserId) {
    GetUserResponse response = new GetUserResponse();
    Optional<User> user = userRepo.findByUserFacebookData_UserId(facebookUserId);
    if (!user.isPresent()) {
      response.setStatus(new Status(Status.USER_NOT_FOUND_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    response.setData(getMinimalInfoAboutUser(user.get()));
    return ResponseEntity.ok(response);
  }

  @GetMapping(path = "/me")
  public ResponseEntity<GetUserResponse> getCurrentUserProfile() {
    GetUserResponse response = new GetUserResponse();
    response.setData(userRepo.findById(getCurrentUser().getUserId()).get());
    return ResponseEntity.ok(response);
  }

  @GetMapping(path = "/me/v2")
  public ResponseEntity<GetUserResponse> getCurrentUserProfileV2() {
    GetUserResponse response = new GetUserResponse();
    User user = userRepo.findById(getCurrentUser().getUserId()).get();
    response.setData(
        User.builder()
            .id(user.getId())
            .username(user.getUsername())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .email(user.getEmail())
            .build());
    return ResponseEntity.ok(response);
  }

  @GetMapping(path = "/publicly_available_users")
  public ResponseEntity<GetPubliclyAvailableUsersResponse> getPubliclyAvailableUsers() {
    GetPubliclyAvailableUsersResponse response = new GetPubliclyAvailableUsersResponse();

    User user = getCurrentUser(userRepo);

    Optional<PubliclyAvailableMaleUser> userAsPubliclyAvailableMale =
        publiclyAvailableMaleUsersRepo.findByUserId(user.getId());

    Optional<PubliclyAvailableFemaleUser> userAsPubliclyAvailableFemale =
        publiclyAvailableFemaleUsersRepo.findByUserId(user.getId());

    if (!userAsPubliclyAvailableMale.isPresent() && !userAsPubliclyAvailableFemale.isPresent()) {
      response.setStatus(new Status(Status.USER_NOT_ADDED_TO_PUBLICLY_AVAILABLE_USERS_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    if (userAsPubliclyAvailableMale.isPresent()) {
      List<PubliclyAvailableUser> publiclyAvailableUsers =
          publiclyAvailableMaleUsersRepo
              .findAll()
              .stream()
              // TODO(issue/335): Optimize filtering publicly available users by the ones without a
              //  friend request sent
              .filter(publiclyAvailableMaleUser ->
                  !publiclyAvailableMaleUser.getUserId().equals(user.getId()) && !friendshipRepo
                      .findByUserId(publiclyAvailableMaleUser.getUserId())
                      .getFriends().stream().anyMatch(friend ->
                          friend.getUserId().equals(user.getId())
                      )
              )
              .map(publiclyAvailableMaleUser -> PubliclyAvailableUser.builder()
                  .userId(publiclyAvailableMaleUser.getUserId())
                  .firstName(publiclyAvailableMaleUser.getFirstName())
                  .lastName(publiclyAvailableMaleUser.getLastName())
                  .build())
              .collect(Collectors.toList());
      response.setData(publiclyAvailableUsers);
      return ResponseEntity.ok(response);
    }

    List<PubliclyAvailableUser> publiclyAvailableUsers =
        publiclyAvailableFemaleUsersRepo
            .findAll()
            .stream()
            .filter(publiclyAvailableFemaleUser ->
                !publiclyAvailableFemaleUser.getUserId().equals(user.getId()) && !friendshipRepo
                    .findByUserId(publiclyAvailableFemaleUser.getUserId())
                    .getFriends().stream().anyMatch(friend ->
                        friend.getUserId().equals(user.getId())
                    )
            )
            .map(publiclyAvailableFemaleUser -> PubliclyAvailableUser.builder()
                .userId(publiclyAvailableFemaleUser.getUserId())
                .firstName(publiclyAvailableFemaleUser.getFirstName())
                .lastName(publiclyAvailableFemaleUser.getLastName())
                .build())
            .collect(Collectors.toList());
    response.setData(publiclyAvailableUsers);
    return ResponseEntity.ok(response);
  }

  @PutMapping(path = "/publicly_available_males")
  public ResponseEntity<AddToPubliclyAvailableUsersResponse> addToPubliclyAvailableMales() {
    AddToPubliclyAvailableUsersResponse response = new AddToPubliclyAvailableUsersResponse();

    User user = getCurrentUser(userRepo);

    Optional<PubliclyAvailableMaleUser> userAsPubliclyAvailableMale =
        publiclyAvailableMaleUsersRepo.findByUserId(user.getId());

    Optional<PubliclyAvailableFemaleUser> userAsPubliclyAvailableFemale =
        publiclyAvailableFemaleUsersRepo.findByUserId(user.getId());

    if (userAsPubliclyAvailableMale.isPresent() || userAsPubliclyAvailableFemale.isPresent()) {
      response.setStatus(new Status(Status.USER_ALREADY_IS_PUBLICLY_AVAILABLE_USER_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    publiclyAvailableMaleUsersRepo
        .save(
            PubliclyAvailableMaleUser.builder()
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build());
    return ResponseEntity.ok(response);
  }

  @PutMapping(path = "/publicly_available_females")
  public ResponseEntity<AddToPubliclyAvailableUsersResponse> addToPubliclyAvailableFemales() {
    AddToPubliclyAvailableUsersResponse response = new AddToPubliclyAvailableUsersResponse();

    User user = getCurrentUser(userRepo);

    Optional<PubliclyAvailableMaleUser> userAsPubliclyAvailableMale =
        publiclyAvailableMaleUsersRepo.findByUserId(user.getId());

    Optional<PubliclyAvailableFemaleUser> userAsPubliclyAvailableFemale =
        publiclyAvailableFemaleUsersRepo.findByUserId(user.getId());

    if (userAsPubliclyAvailableMale.isPresent() || userAsPubliclyAvailableFemale.isPresent()) {
      response.setStatus(new Status(Status.USER_ALREADY_IS_PUBLICLY_AVAILABLE_USER_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    publiclyAvailableFemaleUsersRepo
        .save(
            PubliclyAvailableFemaleUser.builder()
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build());
    return ResponseEntity.ok(response);
  }

  @DeleteMapping(path = "/publicly_available_users")
  public ResponseEntity<DeleteFromPubliclyAvailableUsers> deleteFromPubliclyAvailableUsers() {
    DeleteFromPubliclyAvailableUsers response = new DeleteFromPubliclyAvailableUsers();

    User user = getCurrentUser(userRepo);

    Optional<PubliclyAvailableMaleUser> userAsPubliclyAvailableMale =
        publiclyAvailableMaleUsersRepo.findByUserId(user.getId());

    if (userAsPubliclyAvailableMale.isPresent()) {
      publiclyAvailableMaleUsersRepo.deleteById(userAsPubliclyAvailableMale.get().getId());
      return ResponseEntity.ok(response);
    }

    Optional<PubliclyAvailableFemaleUser> userAsPubliclyAvailableFemale =
        publiclyAvailableFemaleUsersRepo.findByUserId(user.getId());
    if (userAsPubliclyAvailableFemale.isPresent()) {
      publiclyAvailableFemaleUsersRepo.deleteById(userAsPubliclyAvailableFemale.get().getId());
      return ResponseEntity.ok(response);
    }

    response.setStatus(new Status(Status.USER_NOT_ADDED_TO_PUBLICLY_AVAILABLE_USERS_ERROR));
    return ResponseEntity.badRequest().body(response);
  }

  private User getMinimalInfoAboutUser(User user) {
    return User.builder()
        .id(user.getId())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .username(user.getUsername())
        .build();
  }
}
