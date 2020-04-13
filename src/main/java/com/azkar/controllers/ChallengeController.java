package com.azkar.controllers;

import com.azkar.entities.Challenge;
import com.azkar.entities.User;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.challengecontroller.AddPersonalChallengeRequest;
import com.azkar.payload.challengecontroller.AddPersonalChallengeResponse;
import com.azkar.payload.exceptions.BadRequestException;
import com.azkar.repos.UserRepo;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/challenges", produces = MediaType.APPLICATION_JSON_VALUE)
public class ChallengeController extends BaseController {

  @Autowired
  UserRepo userRepo;

  @PostMapping(path = "/personal", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<AddPersonalChallengeResponse> addPersonalChallenge(
      @RequestBody AddPersonalChallengeRequest request) {
    AddPersonalChallengeResponse response = new AddPersonalChallengeResponse();
    // TODO(issue#36): Allow this method to throw the exception and handle it on a higher level.
    try {
      request.validate();
    } catch (BadRequestException e) {
      response.setError(new Error(e.getMessage()));
      return ResponseEntity.badRequest().body(response);
    }
    Challenge challenge = Challenge.builder()
        .name(request.getName())
        .motivation(request.getMotivation())
        .expiryDate(request.getExpiryDate())
        .subChallenges(request.getSubChallenges())
        .usersAccepted(ImmutableList.of(getCurrentUser().getUserId()))
        .creatingUserId(getCurrentUser().getUserId())
        .createdAt(Instant.now().getEpochSecond())
        .modifiedAt(Instant.now().getEpochSecond())
        .build();
    Optional<User> loggedInUser = userRepo.findById(getCurrentUser().getUserId());
    if (!loggedInUser.isPresent()) {
      response.setError(new Error(AddPersonalChallengeResponse.USER_NOT_LOGGED_IN_ERROR));
      return ResponseEntity.badRequest().body(response);
    }
    loggedInUser.get().getPersonalGroup().getChallenges().add(challenge);
    userRepo.save(loggedInUser.get());
    response.setData(challenge);
    return ResponseEntity.ok(response);
  }
}
