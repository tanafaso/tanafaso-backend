package com.omar.azkar.controllers;

import com.omar.azkar.entities.User;
import com.omar.azkar.repos.UserRepo;
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

  @GetMapping(path = "/user/{id}", produces = "application/json")
  public UserControllerResponse getUser(@PathVariable Integer id) {
    Optional<User> user = userRepo.findById(id);
    return new UserControllerResponse(user.isPresent(), user.orElse(null));
  }

  @PostMapping(path = "/user", consumes = "application/json", produces = "application/json")
  public User addUser(@RequestBody User user) {
    User newUser = new User();
    newUser.setName(user.getName());
    userRepo.save(newUser);
    return newUser;
  }

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
