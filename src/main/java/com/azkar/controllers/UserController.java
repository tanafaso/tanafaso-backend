package com.azkar.controllers;

import com.azkar.entities.User;
import com.azkar.payload.usercontroller.AddUserResponse;
import com.azkar.payload.usercontroller.GetUserResponse;
import com.azkar.payload.usercontroller.GetUsersResponse;
import com.azkar.repos.UserRepo;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController extends BaseController {

  @Autowired
  private UserRepo userRepo;

  @GetMapping
  public ResponseEntity<GetUsersResponse> getUsers() {
    GetUsersResponse response = new GetUsersResponse();
    response.setData(userRepo.findAll());
    return ResponseEntity.ok(response);
  }

  @GetMapping(path = "/{id}")
  public ResponseEntity<GetUserResponse> getUser(@PathVariable String id) {
    Optional<User> user = userRepo.findById(id);
    if (!user.isPresent()) {
      return ResponseEntity.notFound().build();
    }
    GetUserResponse response = new GetUserResponse();
    response.setData(user.get());
    return ResponseEntity.ok(response);
  }

  @PostMapping(path = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<AddUserResponse> addUser(@RequestBody User user) {
    User newUser = User.builder().name(user.getName()).email(user.getEmail()).build();
    userRepo.save(newUser);
    AddUserResponse response = new AddUserResponse();
    response.setData(newUser);
    return ResponseEntity.ok(response);
  }
}
