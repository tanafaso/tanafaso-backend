package com.azkar.controllers;

import com.azkar.entities.User;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.homecontroller.GetHomeResponse;
import com.azkar.repos.UserRepo;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class HomeController extends BaseController {

  @Autowired
  public UserRepo userRepo;

  @GetMapping(value = "/")
  public ResponseEntity<GetHomeResponse> getHome() {
    GetHomeResponse response = new GetHomeResponse();

    Optional<User> user = userRepo.findById(getCurrentUser().getUserId());
    if (!user.isPresent()) {
      response.setError(new Error(Error.ERROR_USER_NOT_FOUND));
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    response.setData(user.get());
    return ResponseEntity.ok().body(response);
  }
}
