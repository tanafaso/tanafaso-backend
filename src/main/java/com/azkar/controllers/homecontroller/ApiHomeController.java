package com.azkar.controllers.homecontroller;

import com.azkar.controllers.BaseController;
import com.azkar.entities.Friendship.Friend;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.payload.challengecontroller.responses.GetChallengesV2Response.ReturnedChallenge;
import com.azkar.payload.homecontroller.GetHomeResponse;
import com.azkar.payload.homecontroller.GetHomeResponse.Body;
import com.azkar.repos.UserRepo;
import com.azkar.services.ChallengesService;
import com.azkar.services.FriendshipService;
import com.azkar.services.GroupsService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/apiHome", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiHomeController extends BaseController {

  private static final Logger logger = LoggerFactory.getLogger(ApiHomeController.class);
  private static final int MAX_USER_CHALLENGES_WITH_SAME_TYPE = 10;

  @Autowired
  ChallengesService challengesService;
  @Autowired
  FriendshipService friendshipService;
  @Autowired
  GroupsService groupsService;
  @Autowired
  UserRepo userRepo;

  @GetMapping()
  public ResponseEntity<GetHomeResponse> getHome(
      @RequestHeader(value = API_VERSION_HEADER, required = true) String apiVersion) {
    GetHomeResponse getHomeResponse = new GetHomeResponse();

    User currentUser = getCurrentUser(userRepo);
    List<ReturnedChallenge> challenges =
        challengesService.getAllChallenges(apiVersion, currentUser);
    List<Friend> friendsLeaderboard =
        friendshipService.getFriendsLeaderboard(apiVersion, currentUser);
    List<Group> groups = groupsService.getGroups(currentUser);

    getHomeResponse.setData(Body
        .builder()
        .challenges(challenges)
        .friends(friendsLeaderboard)
        .groups(groups)
        .build()
    );

    cleanOldUserChallengesAsync(currentUser);
    return ResponseEntity.ok(getHomeResponse);
  }

  @Async
  public void cleanOldUserChallengesAsync(User user) {
    logger.info("Deleting old challenges for {} asynchronously", user.getUsername());

    logger.info("{} had {} azkar challenges", user.getUsername(), user.getAzkarChallenges().size());
    user.setAzkarChallenges(user.getAzkarChallenges().subList(
        Math.max(0, user.getAzkarChallenges().size() - MAX_USER_CHALLENGES_WITH_SAME_TYPE),
        user.getAzkarChallenges().size()));

    logger.info("{} had {} reading quran challenges", user.getUsername(),
        user.getReadingQuranChallenges().size());
    user.setReadingQuranChallenges(user.getReadingQuranChallenges().subList(
        Math.max(0, user.getReadingQuranChallenges().size() - MAX_USER_CHALLENGES_WITH_SAME_TYPE),
        user.getReadingQuranChallenges().size()));

    logger.info("{} had {} meaning challenges", user.getUsername(),
        user.getMeaningChallenges().size());
    user.setMeaningChallenges(user.getMeaningChallenges().subList(
        Math.max(0, user.getMeaningChallenges().size() - MAX_USER_CHALLENGES_WITH_SAME_TYPE),
        user.getMeaningChallenges().size()));

    logger.info("{} had {} memorization challenges", user.getUsername(),
        user.getMemorizationChallenges().size());
    user.setMemorizationChallenges(user.getMemorizationChallenges().subList(
        Math.max(0, user.getMemorizationChallenges().size() - MAX_USER_CHALLENGES_WITH_SAME_TYPE),
        user.getMemorizationChallenges().size()));
    userRepo.save(user);
  }
}
