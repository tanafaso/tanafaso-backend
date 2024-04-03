package com.azkar.controllers.challengecontroller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.TestBase;
import com.azkar.controllers.utils.JsonHandler;
import com.azkar.entities.User;
import com.azkar.entities.challenges.AzkarChallenge;
import com.azkar.entities.challenges.GlobalChallenge;
import com.azkar.factories.entities.ChallengeFactory;
import com.azkar.payload.challengecontroller.responses.FinishGlobalChallengeResponse;
import com.azkar.payload.challengecontroller.responses.GetGlobalChallengeResponse;
import com.azkar.payload.challengecontroller.responses.GetGlobalChallengeResponse.ReturnedChallenge;
import com.azkar.payload.usercontroller.responses.GetPubliclyAvailableUsersResponse;
import com.azkar.payload.usercontroller.responses.GetPubliclyAvailableUsersResponse.PubliclyAvailableUser;
import com.azkar.repos.AzkarChallengeRepo;
import com.azkar.repos.GlobalChallengeRepo;
import com.azkar.repos.UserRepo;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class GlobalChallengeTest extends TestBase {

  @Autowired
  UserRepo userRepo;

  @Autowired
  AzkarChallengeRepo azkarChallengeRepo;

  @Autowired
  GlobalChallengeRepo globalChallengeRepo;

  @After
  public void afterEach() {
    globalChallengeRepo.deleteAll(globalChallengeRepo.findAll());
  }

  @Test
  public void getGlobalChallenge_normalScenario_shouldSucceed() throws Exception {
    AzkarChallenge unusedAzkarChallenge1 = ChallengeFactory.getNewChallenge("unused_challenge1",
        "unused_group_id1");
    AzkarChallenge azkarChallenge = ChallengeFactory.getNewChallenge("used_challenge",
        "unneeded_group_id2");
    AzkarChallenge unusedAzkarChallenge2 = ChallengeFactory.getNewChallenge("unused_challenge2",
        "unused_group_id3");

    azkarChallengeRepo.save(unusedAzkarChallenge1);
    azkarChallengeRepo.save(azkarChallenge);
    azkarChallengeRepo.save(unusedAzkarChallenge2);

    globalChallengeRepo.save(
        GlobalChallenge.builder()
            .azkarChallengeIdRef(azkarChallenge.getId())
            .build()
    );

    User user = getNewRegisteredUser();
    GetGlobalChallengeResponse expectedResponse = new GetGlobalChallengeResponse();
    expectedResponse.setData(ReturnedChallenge.builder()
        .azkarChallenge(azkarChallenge)
        .finishedCount(0)
        .build());
    httpClient.performGetRequest(user, "/challenges/global")
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void getGlobalChallenge_usersFinishedChallenge_shouldSucceed() throws Exception {
    AzkarChallenge unusedAzkarChallenge1 = ChallengeFactory.getNewChallenge("unused_challenge1",
        "unused_group_id1");
    AzkarChallenge azkarChallenge = ChallengeFactory.getNewChallenge("used_challenge",
        "unneeded_group_id2");
    AzkarChallenge unusedAzkarChallenge2 = ChallengeFactory.getNewChallenge("unused_challenge2",
        "unused_group_id3");

    azkarChallengeRepo.save(unusedAzkarChallenge1);
    azkarChallengeRepo.save(azkarChallenge);
    azkarChallengeRepo.save(unusedAzkarChallenge2);

    globalChallengeRepo.save(
        GlobalChallenge.builder()
            .azkarChallengeIdRef(azkarChallenge.getId())
            .build()
    );

    User user1 = getNewRegisteredUser();
    User user2 = getNewRegisteredUser();

    // User1 finishes the challenge twice.
    FinishGlobalChallengeResponse expectedFinishGlobalChallengeReponse =
        new FinishGlobalChallengeResponse();
    httpClient.performPutRequest(user1, "/challenges/global")
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedFinishGlobalChallengeReponse)));
    httpClient.performPutRequest(user1, "/challenges/global")
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedFinishGlobalChallengeReponse)));

    // Users retrieve the global challenge.
    GetGlobalChallengeResponse expectedGetGlobalChallengeReponse = new GetGlobalChallengeResponse();
    expectedGetGlobalChallengeReponse.setData(ReturnedChallenge.builder()
        .azkarChallenge(azkarChallenge)
        .finishedCount(2)
        .build());
    httpClient.performGetRequest(user1, "/challenges/global")
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedGetGlobalChallengeReponse)));
    httpClient.performGetRequest(user2, "/challenges/global")
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedGetGlobalChallengeReponse)));

    // User two finishes the global challenge.
    httpClient.performPutRequest(user2, "/challenges/global")
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedFinishGlobalChallengeReponse)));

    // Users retrieve the global challenge.
    expectedGetGlobalChallengeReponse.setData(ReturnedChallenge.builder()
        .azkarChallenge(azkarChallenge)
        .finishedCount(3)
        .build());
    httpClient.performGetRequest(user1, "/challenges/global")
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedGetGlobalChallengeReponse)));
    httpClient.performGetRequest(user2, "/challenges/global")
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedGetGlobalChallengeReponse)));
  }
}
