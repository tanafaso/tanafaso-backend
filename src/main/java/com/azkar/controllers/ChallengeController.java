package com.azkar.controllers;

import com.azkar.entities.Challenge;
import com.azkar.entities.Group;
import com.azkar.entities.PersonalChallenge;
import com.azkar.entities.User;
import com.azkar.entities.User.UserChallengeStatus;
import com.azkar.entities.User.UserSubChallenge;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.challengecontroller.requests.AddChallengeRequest;
import com.azkar.payload.challengecontroller.requests.AddPersonalChallengeRequest;
import com.azkar.payload.challengecontroller.requests.UpdateChallengeRequest;
import com.azkar.payload.challengecontroller.requests.UpdateChallengeRequest.ModifiedSubChallenge;
import com.azkar.payload.challengecontroller.responses.AddChallengeResponse;
import com.azkar.payload.challengecontroller.responses.AddPersonalChallengeResponse;
import com.azkar.payload.challengecontroller.responses.GetChallengeResponse;
import com.azkar.payload.challengecontroller.responses.GetChallengesResponse;
import com.azkar.payload.challengecontroller.responses.GetChallengesResponse.UserChallenge;
import com.azkar.payload.challengecontroller.responses.UpdateChallengeResponse;
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
import java.util.UUID;
import java.util.function.Predicate;
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
import org.springframework.web.bind.annotation.PutMapping;
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

  private static Predicate<UserChallengeStatus> withIsOngoing(boolean isOngoing) {
    return (userChallengeStatus -> userChallengeStatus.isOngoing() == isOngoing);
  }

  private static Predicate<UserChallengeStatus> withIsOngoingAndInGroup(boolean isOngoing,
      Group group) {
    return withIsOngoing(isOngoing)
        .and(userChallengeStatus -> userChallengeStatus.getGroupId().equals(group.getId()));
  }

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
        .id(UUID.randomUUID().toString())
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
    User loggedInUser = getCurrentUser(userRepo);
    loggedInUser.getPersonalChallenges().add(PersonalChallenge.getInstance(challenge));
    userRepo.save(loggedInUser);
    response.setData(challenge);
    return ResponseEntity.ok(response);
  }

  @GetMapping(path = "/personal")
  public ResponseEntity<GetChallengesResponse> getPersonalChallenges() {
    GetChallengesResponse response = new GetChallengesResponse();
    List<PersonalChallenge> personalChallenges = getCurrentUser(userRepo).getPersonalChallenges();
    response.setData(
        personalChallenges.stream().map(this::constructPersonalUserChallenge).collect(
            Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  private UserChallenge constructPersonalUserChallenge(PersonalChallenge challenge) {
    UserChallengeStatus userChallengeStatus = UserChallengeStatus.builder()
        .challengeId(challenge.getId()).isAccepted(true).isOngoing(true)
        .subChallenges(challenge.getUserSubChallenges())
        .build();
    return new UserChallenge(challenge.getChallenge(), userChallengeStatus);
  }

  @PutMapping(path = "/personal/{challengeId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<UpdateChallengeResponse> updatePersonalChallenge(
      @PathVariable(value = "challengeId") String challengeId,
      @RequestBody UpdateChallengeRequest request) {
    User currentUser = getCurrentUser(userRepo);
    Optional<PersonalChallenge> personalChallenge = currentUser.getPersonalChallenges().stream()
        .filter(personalChallengeItem -> personalChallengeItem.getId().equals(challengeId))
        .findAny();
    if (!personalChallenge.isPresent()) {
      UpdateChallengeResponse response = new UpdateChallengeResponse();
      response.setError(new Error(UpdateChallengeResponse.CHALLENGE_NOT_FOUND_ERROR));
      return ResponseEntity.badRequest().body(response);
    }
    List<UserSubChallenge> oldSubChallenges = personalChallenge.get().getUserSubChallenges();
    Optional<ResponseEntity<UpdateChallengeResponse>> errorResponse = updateSubChallenges(
        oldSubChallenges, request);
    if (errorResponse.isPresent()) {
      return errorResponse.get();
    }
    userRepo.save(currentUser);
    return ResponseEntity.ok().build();
  }

  private Optional<ResponseEntity<UpdateChallengeResponse>> updateSubChallenges(
      List<UserSubChallenge> subChallenges, UpdateChallengeRequest request) {
    List<ModifiedSubChallenge> allModifiedSubChallenges = request.getAllModifiedSubChallenges();
    for (ModifiedSubChallenge modifiedSubChallenge : allModifiedSubChallenges) {
      UpdateChallengeResponse response = new UpdateChallengeResponse();
      Optional<UserSubChallenge> subChallenge = findSubChallenge(subChallenges,
          modifiedSubChallenge);
      if (!subChallenge.isPresent()) {
        response.setError(new Error(UpdateChallengeResponse.NON_EXISTENT_SUB_CHALLENGE_ERROR));
        return Optional.of(ResponseEntity.unprocessableEntity().body(response));
      }
      Optional<String> errorMessage = updateSubChallenge(subChallenge.get(), modifiedSubChallenge);
      if (errorMessage.isPresent()) {
        response.setError(new Error(errorMessage.get()));
        return Optional.of(ResponseEntity.unprocessableEntity().body(response));
      }
    }
    return Optional.empty();
  }

  private Optional<UserSubChallenge> findSubChallenge(List<UserSubChallenge> oldSubChallenges,
      ModifiedSubChallenge modifiedSubChallenge) {
    for (UserSubChallenge subChallenge : oldSubChallenges) {
      if (subChallenge.getZekrId().equals(modifiedSubChallenge.getZekrId())) {
        return Optional.of(subChallenge);
      }
    }
    return Optional.empty();
  }

  /**
   * Tries updating the subChallenge as requested in modifiedSubChallenge. If an error occurred the
   * function returns a String with the error message, and returns empty object otherwise.
   */
  private Optional<String> updateSubChallenge(UserSubChallenge subChallenge,
      ModifiedSubChallenge modifiedSubChallenge) {
    int newLeftRepetitions = modifiedSubChallenge.getNewLeftRepetitions();
    if (newLeftRepetitions > subChallenge.getLeftRepetitions()) {
      return Optional.of(UpdateChallengeResponse.INCREMENTING_LEFT_REPETITIONS_ERROR);
    }
    if (newLeftRepetitions < 0) {
      logger.warn("Received UpdateChallenge request with negative leftRepetition value of: "
          + newLeftRepetitions);
      newLeftRepetitions = 0;
    }
    subChallenge.setLeftRepetitions(newLeftRepetitions);
    return Optional.empty();
  }

  @GetMapping("{challengeId}")
  public ResponseEntity<GetChallengeResponse> getChallenge(
      @PathVariable(value = "challengeId") String challengeId) {
    GetChallengeResponse response = new GetChallengeResponse();
    Optional<UserChallengeStatus> userChallengeStatus = getUserChallengeStatus(
        getCurrentUser(userRepo), challengeId);
    if (!userChallengeStatus.isPresent()) {
      response.setError(new Error(GetChallengeResponse.CHALLENGE_NOT_FOUND_ERROR));
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    response.setData(getUserChallenge(userChallengeStatus.get()));
    return ResponseEntity.ok(response);
  }

  private UserChallenge getUserChallenge(UserChallengeStatus userChallengeStatus) {
    Optional<Challenge> challenge = challengeRepo.findById(userChallengeStatus.getChallengeId());
    return UserChallenge.builder().challengeInfo(challenge.get())
        .userChallengeStatus(userChallengeStatus).build();
  }

  private Optional<UserChallengeStatus> getUserChallengeStatus(User user, String challengeId) {
    return user.getUserChallengeStatuses()
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
      response.setError(new Error(AddChallengeResponse.GROUP_NOT_FOUND_ERROR));
      return ResponseEntity.badRequest().body(response);
    }
    if (!groupContainsCurrentUser(group.get())) {
      response.setError(new Error(AddChallengeResponse.NOT_GROUP_MEMBER_ERROR));
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
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

  private boolean groupContainsCurrentUser(Group group) {
    return group.getUsersIds().contains(getCurrentUser().getUserId());
  }

  private void addChallengeToUser(User user, Challenge challenge) {
    UserChallengeStatus userChallengeStatus = UserChallengeStatus.builder()
        .challengeId(challenge.getId())
        .isAccepted(user.getId().equals(getCurrentUser().getUserId()))
        .subChallenges(UserSubChallenge.fromSubChallengesCollection(challenge.getSubChallenges()))
        .isOngoing(challenge.isOngoing())
        .groupId(challenge.getGroupId())
        .build();
    user.getUserChallengeStatuses().add(userChallengeStatus);
  }

  @GetMapping(path = "/ongoing")
  public ResponseEntity<GetChallengesResponse> getOngoingChallenges() {
    return getChallenges(withIsOngoing(true));
  }

  @GetMapping(path = "/proposed")
  public ResponseEntity<GetChallengesResponse> getProposedChallenges() {
    return getChallenges(withIsOngoing(false));
  }

  @GetMapping(path = "/groups/{groupId}/ongoing")
  public ResponseEntity<GetChallengesResponse> getGroupOngoingChallenges(
      @PathVariable(value = "groupId") String groupId) {
    return getGroupChallenge(groupId, /* isOngoing= */ true);
  }

  @GetMapping(path = "/groups/{groupId}/proposed")
  public ResponseEntity<GetChallengesResponse> getGroupProposedChallenges(
      @PathVariable(value = "groupId") String groupId) {
    return getGroupChallenge(groupId, /* isOngoing= */ false);
  }

  private ResponseEntity<GetChallengesResponse> getGroupChallenge(
      @PathVariable("groupId") String groupId, boolean isOngoing) {
    Optional<Group> optionalGroup = groupRepo.findById(groupId);
    ResponseEntity<GetChallengesResponse> error = validateGroupAndReturnErrors(optionalGroup);
    if (error != null) {
      return error;
    }
    return getChallenges(withIsOngoingAndInGroup(isOngoing, optionalGroup.get()));
  }

  private ResponseEntity<GetChallengesResponse> validateGroupAndReturnErrors(
      Optional<Group> optionalGroup) {
    GetChallengesResponse response = new GetChallengesResponse();
    if (!optionalGroup.isPresent()) {
      response.setError(new Error(GetChallengesResponse.GROUP_NOT_FOUND_ERROR));
      return ResponseEntity.badRequest().body(response);
    }
    if (!groupContainsCurrentUser(optionalGroup.get())) {
      response.setError(new Error(GetChallengesResponse.NON_GROUP_MEMBER_ERROR));
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
    return null;
  }

  private ResponseEntity<GetChallengesResponse> getChallenges(
      Predicate<UserChallengeStatus> userChallengeStatusesFilter) {
    GetChallengesResponse response = new GetChallengesResponse();
    List<UserChallenge> userChallenges = getCurrentUser(userRepo)
        .getUserChallengeStatuses().stream()
        .filter(userChallengeStatusesFilter)
        .map(this::constructUserChallenge)
        .collect(Collectors.toList());
    response.setData(userChallenges);
    return ResponseEntity.ok(response);
  }

  private UserChallenge constructUserChallenge(UserChallengeStatus userChallengeStatus) {
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

  @PutMapping(path = "/{challengeId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<UpdateChallengeResponse> updateChallenge(
      @PathVariable(value = "challengeId") String challengeId,
      @RequestBody UpdateChallengeRequest request) {
    User currentUser = getCurrentUser(userRepo);
    Optional<UserChallengeStatus> currentUserChallenge = getUserChallengeStatus(currentUser,
        challengeId);
    if (!currentUserChallenge.isPresent()) {
      UpdateChallengeResponse response = new UpdateChallengeResponse();
      response.setError(new Error(UpdateChallengeResponse.CHALLENGE_NOT_FOUND_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    List<UserSubChallenge> oldSubChallenges = currentUserChallenge.get().getSubChallenges();
    Optional<ResponseEntity<UpdateChallengeResponse>> errorResponse = updateSubChallenges(
        oldSubChallenges, request);
    if (errorResponse.isPresent()) {
      return errorResponse.get();
    }
    userRepo.save(currentUser);
    return ResponseEntity.ok().build();
  }
}
