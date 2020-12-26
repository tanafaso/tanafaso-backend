package com.azkar;


import com.azkar.controllers.utils.AzkarApi;
import com.azkar.controllers.utils.HttpClient;
import com.azkar.entities.User;
import com.azkar.factories.entities.UserFactory;
import com.azkar.services.UserService;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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

  @After
  public final void afterBase() {
    mongoTemplate.getDb().drop();
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

  protected String mapToJson(Object obj) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(Include.NON_NULL);
    return objectMapper.writeValueAsString(obj);
  }

  protected <T> T mapFromJson(String json, TypeReference<T> c) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.readValue(json, c);
  }

  protected <T> T mapFromJson(String json, Class<T> c) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.readValue(json, c);
  }

  protected User getNewRegisteredUser() {
    User newUser = UserFactory.getNewUser();
    addNewUser(newUser);
    return newUser;
  }
}
