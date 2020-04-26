package com.azkar.controllers;

import static com.azkar.payload.challengecontroller.requests.AddChallengeRequest.GROUP_NOT_FOUND_ERROR;

import com.azkar.entities.Challenge;
import com.azkar.entities.Challenge.SubChallenges;
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
    Optional<Group> group = groupRepo.findById(req.getChallenge().getGroupId());
    if (!group.isPresent()) {
      response.setError(new Error(GROUP_NOT_FOUND_ERROR));
      return ResponseEntity.badRequest().body(response);
    }
    List<String> groupUsersIds = group.get().getUsersIds();
    ArrayList<String> usersAccepted = new ArrayList(Arrays.asList(getCurrentUser().getUserId()));
    Challenge challenge = req.getChallenge().toBuilder()
        .creatingUserId(getCurrentUser().getUserId())
        .isOngoing(groupUsersIds.size() == 1)
        .usersAccepted(usersAccepted)
        .build();
    challengeRepo.save(challenge);

    group.get().getChallengesIds().add(challenge.getId());
    groupRepo.save(group.get());

    Iterable<User> affectedUsers = userRepo.findAllById(groupUsersIds);
    affectedUsers.forEach(user -> addChallengeToUser(user,
        req.getChallenge().getSubChallenges(),
        challenge.getId()));
    userRepo.saveAll(affectedUsers);

    response.setData(challenge);
    return ResponseEntity.ok(response);
  }

  private void addChallengeToUser(
      User user,
      List<SubChallenges> subChallenges,
      String challengeId) {
    UserChallenge userChallenge = UserChallenge.builder()
        .challengeId(challengeId)
        .isAccepted(user.getId().equals(getCurrentUser().getUserId()))
        .subChallenges(subChallenges)
        .build();
    user.getUserChallenges().add(userChallenge);
  }
}
