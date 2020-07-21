package com.azkar.controllers;

import static com.azkar.payload.challengecontroller.requests.AddChallengeRequest.GROUP_NOT_FOUND_ERROR;

import com.azkar.entities.Challenge;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.entities.User.UserChallengeStatus;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.challengecontroller.requests.AddChallengeRequest;
import com.azkar.payload.challengecontroller.requests.AddPersonalChallengeRequest;
import com.azkar.payload.challengecontroller.responses.AddChallengeResponse;
import com.azkar.payload.challengecontroller.responses.AddPersonalChallengeResponse;
import com.azkar.payload.challengecontroller.responses.GetChallengeResponse;
import com.azkar.payload.challengecontroller.responses.GetChallengesResponse;
import com.azkar.payload.challengecontroller.responses.GetChallengesResponse.UserReturnedChallenge;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerErrorException;

@RestController
@RequestMapping(path = "/challenges", produces = MediaType.APPLICATION_JSON_VALUE)
public class ChallengeController extends BaseController {

  private static final Logger logger = LoggerFactory.getLogger(ChallengeController.class);
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
        .isOngoing(true)
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

  @GetMapping("{challengeId}")
  public ResponseEntity<GetChallengeResponse> getChallenge(
      @PathVariable(value = "challengeId") String challengeId) {
    GetChallengeResponse response = new GetChallengeResponse();
    Optional<UserChallengeStatus> userChallengeStatus = getCurrentUserChallengeStatus(challengeId);
    if (!userChallengeStatus.isPresent()) {
      return ResponseEntity.notFound().build();
    }
    response.setData(getUserReturnedChallenge(userChallengeStatus.get()));
    return ResponseEntity.ok(response);
  }

  private Optional<UserChallengeStatus> getCurrentUserChallengeStatus(String challengeId) {
    User currentUser = userRepo.findById(getCurrentUser().getUserId()).get();
    return currentUser.getUserChallengeStatuses()
        .stream()
        .filter(challengeStatus -> challengeStatus.getChallengeId().equals(challengeId))
        .findFirst();
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
    affectedUsers.forEach(user -> addChallengeToUser(user, challenge));
    userRepo.saveAll(affectedUsers);

    response.setData(challenge);
    return ResponseEntity.ok(response);
  }

  private void addChallengeToUser(User user, Challenge challenge) {
    UserChallengeStatus userChallengeStatus = UserChallengeStatus.builder()
        .challengeId(challenge.getId())
        .isAccepted(user.getId().equals(getCurrentUser().getUserId()))
        .subChallenges(challenge.getSubChallenges())
        .isOngoing(challenge.isOngoing())
        .build();
    user.getUserChallengeStatuses().add(userChallengeStatus);
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
    List<UserReturnedChallenge> userReturnedChallenges = userRepo
        .findById(getCurrentUser().getUserId()).get()
        .getUserChallengeStatuses().stream()
        .filter(userChallengeStatus -> userChallengeStatus.isOngoing() == isOngoing)
        .map(this::getUserReturnedChallenge)
        .collect(Collectors.toList());
    response.setData(userReturnedChallenges);
    return ResponseEntity.ok(response);
  }

  private UserReturnedChallenge getUserReturnedChallenge(UserChallengeStatus userChallengeStatus) {
    Optional<Challenge> challengeInfo = challengeRepo
        .findById(userChallengeStatus.getChallengeId());
    if (!challengeInfo.isPresent()) {
      logger
          .error("Challenge {} found in User {} entity and without corresponding challenge entity.",
              userChallengeStatus.getChallengeId(), getCurrentUser().getUserId());
      throw new ServerErrorException(DANGLING_USER_CHALLENGE_LINK_ERROR,
          new Throwable(DANGLING_USER_CHALLENGE_LINK_ERROR));
    }
    return UserReturnedChallenge.builder()
        .userChallengeStatus(userChallengeStatus)
        .challengeInfo(challengeInfo.get())
        .build();
  }
}
