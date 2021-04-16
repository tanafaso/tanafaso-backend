package com.azkar.controllers;

import com.azkar.entities.User;
import com.azkar.payload.ResponseBase.Status;
import com.azkar.payload.usercontroller.GetUserResponse;
import com.azkar.repos.UserRepo;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping(path = "users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController extends BaseController {

  @Autowired
  private UserRepo userRepo;

  @GetMapping(path = "/{id}")
  public ResponseEntity<GetUserResponse> getUser(@PathVariable String id) {
    GetUserResponse response = new GetUserResponse();

    Optional<User> user = userRepo.findById(id);
    if (!user.isPresent()) {
      response.setStatus(new Status(Status.USER_NOT_FOUND_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    response.setData(user.get());
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

  private ResponseEntity<GetUserResponse> searchForUserByUsername(String username) {
    Optional<User> user = userRepo.findByUsername(username);
    GetUserResponse response = new GetUserResponse();
    if (!user.isPresent()) {
      response.setStatus(new Status(Status.USER_NOT_FOUND_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
    }
    response.setData(user.get());
    return ResponseEntity.ok(response);
  }

  private ResponseEntity<GetUserResponse> searchForUserByFacebookUserId(String facebookUserId) {
    GetUserResponse response = new GetUserResponse();
    Optional<User> user = userRepo.findByUserFacebookData_UserId(facebookUserId);
    if (!user.isPresent()) {
      response.setStatus(new Status(Status.USER_NOT_FOUND_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    response.setData(user.get());
    return ResponseEntity.ok(response);
  }

  @GetMapping(path = "/me")
  public ResponseEntity<GetUserResponse> getCurrentUserProfile() {
    GetUserResponse response = new GetUserResponse();
    response.setData(userRepo.findById(getCurrentUser().getUserId()).get());
    return ResponseEntity.ok(response);
  }
}
