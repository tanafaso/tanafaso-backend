package com.omar.azkar.controllers;

import com.omar.azkar.entities.User;
import com.omar.azkar.repos.UserRepo;
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
