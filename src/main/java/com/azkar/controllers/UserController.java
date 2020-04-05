package com.azkar.controllers;

import com.azkar.entities.Friendship;
import com.azkar.entities.User;
import com.azkar.payload.usercontroller.AddUserResponse;
import com.azkar.payload.usercontroller.GetFriendsResponse;
import com.azkar.payload.usercontroller.GetUserResponse;
import com.azkar.payload.usercontroller.GetUsersResponse;
import com.azkar.repos.FriendshipRepo;
import com.azkar.repos.UserRepo;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController extends BaseController {

  @Autowired
  private UserRepo userRepo;

  @Autowired
  private FriendshipRepo friendshipRepo;

  @GetMapping(path = "/users", produces = "application/json")
  public ResponseEntity<GetUsersResponse> getUsers() {
    GetUsersResponse response = new GetUsersResponse();
    response.setData(userRepo.findAll());
    return ResponseEntity.ok(response);
  }

  @GetMapping(path = "/user/{id}", produces = "application/json")
  public ResponseEntity<GetUserResponse> getUser(@PathVariable String id) {
    Optional<User> user = userRepo.findById(id);
    if (!user.isPresent()) {
      return ResponseEntity.notFound().build();
    }
    GetUserResponse response = new GetUserResponse();
    response.setData(user.get());
    return ResponseEntity.ok(response);
  }

  @PostMapping(path = "/user", consumes = "application/json", produces = "application/json")
  public ResponseEntity<AddUserResponse> addUser(@RequestBody User user) {
    User newUser = new User();
    newUser.setName(user.getName());
    newUser.setEmail(user.getEmail());
    userRepo.save(newUser);
    AddUserResponse response = new AddUserResponse();
    response.setData(newUser);
    return ResponseEntity.ok(response);
  }

  @GetMapping(path = "/users/friends")
  public ResponseEntity<GetFriendsResponse> getFriends() {
    List<Friendship> friendshipList = friendshipRepo.findByUserId1(currentUser.getId());

    GetFriendsResponse getFriendsResponse = new GetFriendsResponse();
    for (Friendship friendship : friendshipList) {
      getFriendsResponse.getFriendshipStatusList().add(
          GetFriendsResponse.FriendshipStatus.builder().userId(friendship.getUserId2())
              .isPending(friendship.isPending()).build());
    }

    return ResponseEntity.ok(getFriendsResponse);
  }
}
