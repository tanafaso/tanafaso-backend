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
import com.azkar.payload.challengecontroller.responses.GetChallengesResponse;
import com.azkar.payload.challengecontroller.responses.utils.UserReturnedChallenge;
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
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerErrorException;

@RestController
@RequestMapping(path = "/challenges", produces = MediaType.APPLICATION_JSON_VALUE)
public class ChallengeController extends BaseController {

  private static final String DANGLING_USER_CHALLENGE_LINK_ERROR =
      "Challenge found in User entity without corresponding challenge entity.";

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
    if (!groupRepo.existsById(req.getChallenge().getGroupId())) {
      response.setError(new Error(GROUP_NOT_FOUND_ERROR));
      return ResponseEntity.badRequest().body(response);
    }
    List<String> groupUsersIds = groupRepo.findById(req.getChallenge().getGroupId()).get()
        .getUsersIds();
    ArrayList<String> usersAccepted = new ArrayList(Arrays.asList(getCurrentUser().getUserId()));
    Challenge challenge = Challenge.builder()
        .name(req.getChallenge().getName())
        .groupId(req.getChallenge().getGroupId())
        .creatingUserId(getCurrentUser().getUserId())
        .motivation(req.getChallenge().getMotivation())
        .isOngoing(groupUsersIds.size() == 1)
        .expiryDate(req.getChallenge().getExpiryDate())
        .usersAccepted(usersAccepted)
        .subChallenges(req.getChallenge().getSubChallenges())
        .build();
    challengeRepo.save(challenge);

    Group group = groupRepo.findById(req.getChallenge().getGroupId()).get();
    group.getChallengesIds().add(challenge.getId());
    groupRepo.save(group);

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

  @GetMapping(path = "/ongoing")
  public ResponseEntity<GetChallengesResponse> getOngoingChallenges() {
    return getChallenges(/* isOngoing= */ true);
  }

  @GetMapping(path = "/proposed")
  public ResponseEntity<GetChallengesResponse> getProposedChallenges() {
    return getChallenges(/* isOngoing= */ false);
  }

  private ResponseEntity<GetChallengesResponse> getChallenges(boolean isOngoing) {
    GetChallengesResponse response = new GetChallengesResponse();
    List<UserReturnedChallenge> challengeIds = userRepo.findById(getCurrentUser().getUserId()).get()
        .getUserChallenges().stream()
        .filter(userChallenge -> userChallenge.isOngoing() == isOngoing)
        .map(this::getUserReturnedChallenge)
        .collect(Collectors.toList());
    response.setData(challengeIds);
    return ResponseEntity.ok(response);
  }

  private UserReturnedChallenge getUserReturnedChallenge(UserChallenge userChallenge) {
    Optional<Challenge> challengeInfo = challengeRepo.findById(userChallenge.getChallengeId());
    if (!challengeInfo.isPresent()) {
      throw new ServerErrorException(DANGLING_USER_CHALLENGE_LINK_ERROR,
          new Throwable(DANGLING_USER_CHALLENGE_LINK_ERROR));
    }
    return UserReturnedChallenge.builder()
        .userStatus(userChallenge)
        .challengeInfo(challengeInfo.get())
        .build();
}
