package com.azkar.controllers;

import static com.azkar.payload.challengecontroller.requests.AddChallengeRequest.GROUP_NOT_FOUND_ERROR;

import com.azkar.entities.Challenge;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.entities.User.UserChallenge;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.challengecontroller.requests.AddChallengeRequest;
import com.azkar.payload.challengecontroller.requests.AddPersonalChallengeRequest;
import com.azkar.payload.challengecontroller.responses.AddChallengeResponse;
import com.azkar.payload.challengecontroller.responses.AddPersonalChallengeResponse;
import com.azkar.payload.exceptions.BadRequestException;
import com.azkar.repos.ChallengeRepo;
import com.azkar.repos.GroupRepo;
import com.azkar.repos.UserRepo;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
  @Autowired
  ChallengeRepo challengeRepo;
  @Autowired
  GroupRepo groupRepo;

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
    User loggedInUser = userRepo.findById(getCurrentUser().getUserId()).get();
    loggedInUser.getPersonalChallenges().add(challenge);
    userRepo.save(loggedInUser);
    response.setData(challenge);
    return ResponseEntity.ok(response);
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<AddChallengeResponse> addChallenge(
      @RequestBody AddChallengeRequest req) {
    AddChallengeResponse response = new AddChallengeResponse();
    try {
      req.validate();
    } catch (BadRequestException e) {
      response.setError(new Error(e.getMessage()));
      return ResponseEntity.badRequest().body(response);
    }
    if (!groupRepo.existsById(req.getGroupId())) {
      response.setError(new Error(GROUP_NOT_FOUND_ERROR));
      return ResponseEntity.badRequest().body(response);
    }
    List<String> groupUsers = groupRepo.findById(req.getGroupId()).get().getUsersIds();
    ArrayList<String> usersAccepted = new ArrayList(Arrays.asList(getCurrentUser().getUserId()));
    Challenge challenge = Challenge.builder()
        .name(req.getName())
        .groupId(req.getGroupId())
        .creatingUserId(getCurrentUser().getUserId())
        .motivation(req.getMotivation())
        .isOngoing(groupUsers.size() == 1)
        .expiryDate(req.getExpiryDate())
        .usersAccepted(usersAccepted)
        .subChallenges(req.getSubChallenges())
        .build();
    challengeRepo.save(challenge);

    Group group = groupRepo.findById(req.getGroupId()).get();
    group.getChallengesIds().add(challenge.getId());
    groupRepo.save(group);

    User.UserChallenge userChallenge = UserChallenge.builder()
        .challengeId(challenge.getId())
        .isAccepted(true)
        .subChallenges(req.getSubChallenges())
        .build();
    User user = userRepo.findById(getCurrentUser().getUserId()).get();
    user.getUserChallenges().add(userChallenge);
    userRepo.save(user);

    response.setData(challenge);
    return ResponseEntity.ok(response);
  }
}
