package com.azkar.controllers;

import com.azkar.controllers.responses.GetFriendsResponse;
import com.azkar.entities.Friendship;
import com.azkar.entities.User;
import com.azkar.repos.FriendshipRepo;
import com.azkar.repos.UserRepo;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

  @Autowired
  private UserRepo userRepo;

  @Autowired
  private FriendshipRepo friendshipRepo;

  @GetMapping(path = "/users", produces = "application/json")
  public List<User> getUsers() {
    return userRepo.findAll();
  }

  @GetMapping(path = "/user/{id}", produces = "application/json")
  public UserControllerResponse getUser(@PathVariable String id) {
    Optional<User> user = userRepo.findById(id);
    return new UserControllerResponse(user.isPresent(), user.orElse(null));
  }

  @PostMapping(path = "/user", consumes = "application/json", produces = "application/json")
  public User addUser(@RequestBody User user) {
    User newUser = new User();
    newUser.setName(user.getName());
    newUser.setEmail(user.getEmail());
    userRepo.save(newUser);
    return newUser;
  }

  @GetMapping(path = "/users/friends", produces = "application/json")
  public GetFriendsResponse getFriends(Principal principal) {
    String loggedInUserId = principal.toString();

    List<Friendship> friendshipList = friendshipRepo.findByUserId1(loggedInUserId);

    GetFriendsResponse response = new GetFriendsResponse();
    for (Friendship friendship : friendshipList) {
      GetFriendsResponse.Friend friend = new GetFriendsResponse.Friend(
          friendship.getUserId2(),
          friendship.isPending());
      response.getFriendsList().add(friend);
    }
    return response;
  }

//  @PostMapping(path = "/users/friends/{id}", produces = "application/json")
//  public AddFriendResponse addFriend(Principal principal) {
//
//  }


  private static class UserControllerResponse extends Response {

    User user;

    public UserControllerResponse(boolean success, User user) {
      super(success);
      this.user = user;
    }

    public User getUser() {
      return user;
    }
  }
}
