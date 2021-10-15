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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/apiHome", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiHomeController extends BaseController {

  private static final Logger logger = LoggerFactory.getLogger(ApiHomeController.class);

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

    return ResponseEntity.ok(getHomeResponse);
  }
}