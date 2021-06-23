package com.azkar;


import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.controllers.challengecontroller.PersonalChallengeTest;
import com.azkar.controllers.utils.AzkarApi;
import com.azkar.controllers.utils.HttpClient;
import com.azkar.controllers.utils.JsonHandler;
import com.azkar.entities.Challenge;
import com.azkar.entities.User;
import com.azkar.factories.entities.ChallengeFactory;
import com.azkar.factories.entities.UserFactory;
import com.azkar.payload.challengecontroller.requests.AddPersonalChallengeRequest;
import com.azkar.payload.challengecontroller.responses.AddPersonalChallengeResponse;
import com.azkar.repos.UserRepo;
import com.azkar.services.NotificationsService;
import com.azkar.services.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.ResultActions;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class TestBase {

  @Autowired
  public AzkarApi azkarApi;
  @Autowired
  protected HttpClient httpClient;
  @Autowired
  UserService userService;
  @Autowired
  MongoTemplate mongoTemplate;
  @Autowired
  UserRepo userRepo;
  @MockBean
  NotificationsService notificationsService;

  @Before
  public final void beforeBase() {
    mongoTemplate.getDb().drop();

    Mockito.doNothing().when(notificationsService).
        sendNotificationToUser(any(), any(), any());

    User sabeq = User.builder()
        .id(User.SABEQ_ID)
        .firstName("سابق")
        .lastName("\uD83C\uDFCE️️")
        .username("sabeq")
        .build();
    userRepo.save(sabeq);
  }

  protected void addNewUser(User user) {
    userService.addNewUser(user);
  }

  //TODO(#118): All perform.*Request methods should be moved to httpClient.
  // If you need to use any of these methods, a corresponding method to your use case should be
  // created in AzkarApi and called instead or use HttpClient.perform.*Request if the use case
  // should not belong to AzkarApi.
  @Deprecated
  protected ResultActions performGetRequest(String token, String path) throws Exception {
    return httpClient.performGetRequest(token, path);
  }

  @Deprecated
  protected ResultActions performGetRequest(User user, String path) throws Exception {
    return httpClient.performGetRequest(user, path);
  }

  @Deprecated
  protected ResultActions performPostRequest(User user, String path, String body) throws Exception {
    return httpClient.performPostRequest(user, path, body);
  }

  @Deprecated
  protected ResultActions performPutRequest(String path, String body) throws Exception {
    return httpClient.performPutRequest(path, body);
  }

  @Deprecated
  protected ResultActions performPutRequest(User user, String path, String body) throws Exception {
    return httpClient.performPutRequest(user, path, body);
  }

  @Deprecated
  protected ResultActions performDeleteRequest(User user, String path) throws Exception {
    return httpClient.performDeleteRequest(user, path);
  }

  protected <T> T getResponse(ResultActions resultActions, Class<T> cls)
      throws JsonProcessingException, UnsupportedEncodingException {
    String jsonResponse = resultActions.andReturn().getResponse().getContentAsString();
    return JsonHandler.fromJson(jsonResponse, cls);
  }

  protected User getNewRegisteredUser() {
    User newUser = UserFactory.getNewUser();
    addNewUser(newUser);
    return newUser;
  }

  // TODO(issue#156): Think about a place for helper functions for tests
  protected Challenge createGroupChallenge(User user, String groupId) throws Exception {
    Challenge challenge = ChallengeFactory.getNewChallenge(groupId);
    azkarApi.addChallenge(user, challenge).andExpect(status().isOk());
    return challenge;
  }

  protected Challenge createGroupChallenge(User user, Challenge challenge) throws Exception {
    azkarApi.addChallenge(user, challenge).andExpect(status().isOk());
    return challenge;
  }

  protected Challenge createPersonalChallenge(User user)
      throws Exception {
    long expiryDate = Instant.now().getEpochSecond() + ChallengeFactory.EXPIRY_DATE_OFFSET;
    AddPersonalChallengeRequest request = PersonalChallengeTest
        .createPersonalChallengeRequest(expiryDate);
    return createPersonalChallenge(user, request);
  }

  protected Challenge createPersonalChallenge(User user, AddPersonalChallengeRequest request)
      throws Exception {
    ResultActions response = azkarApi.addPersonalChallenge(user, request)
        .andExpect(status().isOk());
    return getResponse(response, AddPersonalChallengeResponse.class).getData();
  }


}
