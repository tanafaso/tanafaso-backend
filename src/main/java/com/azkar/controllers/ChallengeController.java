package com.azkar.controllers;

import com.azkar.entities.Challenge;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.entities.User.UserChallengeStatus;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.challengecontroller.requests.AddChallengeRequest;
import com.azkar.payload.challengecontroller.requests.AddPersonalChallengeRequest;
import com.azkar.payload.challengecontroller.responses.AddChallengeResponse;
import com.azkar.payload.challengecontroller.responses.AddPersonalChallengeResponse;
import com.azkar.payload.challengecontroller.responses.GetChallengesResponse;
import com.azkar.payload.challengecontroller.responses.GetChallengesResponse.UserChallenge;
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
import org.springframework.http.HttpStatus;
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
      response.setError(new Error(GetChallengesResponse.GROUP_NOT_FOUND_ERROR));
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
        .groupId(challenge.getGroupId())
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

  @GetMapping(path = "/groups/{groupId}/ongoing")
  public ResponseEntity<GetChallengesResponse> getGroupOngoingChallenges(
      @PathVariable(value = "groupId") String groupId) {
    if (groupRepo.existsById(groupId)) {
      return getChallenges(/* isOngoing= */ true, groupId);
    }
    GetChallengesResponse response = new GetChallengesResponse();
    response.setError(new Error(GetChallengesResponse.GROUP_NOT_FOUND_ERROR));
    return ResponseEntity.unprocessableEntity().body(response);
  }

  @GetMapping(path = "/groups/{groupId}/proposed")
  public ResponseEntity<GetChallengesResponse> getGroupProposedChallenges(
      @PathVariable(value = "groupId") String groupId) {
    if (groupRepo.existsById(groupId)) {
      return getChallenges(/* isOngoing= */ false, groupId);
    }
    GetChallengesResponse response = new GetChallengesResponse();
    response.setError(new Error(GetChallengesResponse.GROUP_NOT_FOUND_ERROR));
    return ResponseEntity.unprocessableEntity().body(response);
  }

  private ResponseEntity<GetChallengesResponse> getChallenges(boolean isOngoing) {
    return getChallenges(isOngoing, /* groupId= */ null);
  }

  private ResponseEntity<GetChallengesResponse> getChallenges(boolean isOngoing, String groupId) {
    GetChallengesResponse response = new GetChallengesResponse();
    if (groupId != null && !groupContainsCurrentUser(groupId)) {
      response.setError(new Error(GetChallengesResponse.NON_GROUP_MEMBER_ERROR));
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
    List<UserChallenge> userChallenges = userRepo
        .findById(getCurrentUser().getUserId()).get()
        .getUserChallengeStatuses().stream()
        .filter(
            userChallengeStatus -> userChallengeStatus.isOngoing() == isOngoing
                && nullGroupOrContainsChallenge(groupId, userChallengeStatus))
        .map(this::getUserChallenge)
        .collect(Collectors.toList());
    response.setData(userChallenges);
    return ResponseEntity.ok(response);
  }

  private boolean nullGroupOrContainsChallenge(String groupId,
      UserChallengeStatus userChallengeStatus) {
    return groupId == null || userChallengeStatus.getGroupId().equals(groupId);
  }

  private boolean groupContainsCurrentUser(String groupId) {
    Group group = groupRepo.findById(groupId).get();
    return group.getUsersIds().contains(getCurrentUser().getUserId());
  }

  private UserChallenge getUserChallenge(UserChallengeStatus userChallengeStatus) {
    Optional<Challenge> challengeInfo = challengeRepo
        .findById(userChallengeStatus.getChallengeId());
    if (!challengeInfo.isPresent()) {
      logger
          .error("Challenge {} found in User {} entity and without corresponding challenge entity.",
              userChallengeStatus.getChallengeId(), getCurrentUser().getUserId());
      throw new ServerErrorException(DANGLING_USER_CHALLENGE_LINK_ERROR,
          new Throwable(DANGLING_USER_CHALLENGE_LINK_ERROR));
    }
    return UserChallenge.builder()
        .userChallengeStatus(userChallengeStatus)
        .challengeInfo(challengeInfo.get())
        .build();
  }
}
