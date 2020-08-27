package com.azkar;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.azkar.entities.User;
import com.azkar.factories.entities.UserFactory;
import com.azkar.services.UserService;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class TestBase {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserService userService;

  @Autowired
  private MongoTemplate mongoTemplate;

  @Value("${app.jwtSecret}")
  private String jwtSecret;

  @After
  public final void afterBase() {
    mongoTemplate.getDb().drop();
  }

  protected void addNewUser(User user) {
    userService.addNewUser(user);
  }

  protected User getLoggedInUser() {
    User user = UserFactory.getNewUser();
    addNewUser(user);
    return user;
  }

  private String getAuthenticationToken(User user) throws UnsupportedEncodingException {
    final long TOKEN_TIMEOUT_IN_MILLIS = TimeUnit.MINUTES.toMillis(1);
    return JWT.create()
        .withSubject(user.getId())
        .withExpiresAt(new Date(System.currentTimeMillis() + TOKEN_TIMEOUT_IN_MILLIS))
        .sign(Algorithm.HMAC512(jwtSecret));
  }

  protected ResultActions performGetRequest(User user, String path) throws Exception {
    return performGetRequest(user, path, /*body=*/null);
  }

  protected ResultActions performGetRequest(String token, String path) throws Exception {
    MockHttpServletRequestBuilder requestBuilder = get(path);
    requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);

    return mockMvc.perform(requestBuilder);
  }

  protected ResultActions performGetRequest(User user, String path, String body) throws Exception {
    return mockMvc.perform(addRequestBodyAndToken(get(path), user, body));
  }

  protected ResultActions performPostRequest(User user, String path, String body) throws Exception {
    return mockMvc.perform(addRequestBodyAndToken(post(path), user, body));
  }

  protected ResultActions performPutRequest(String path, String body) throws Exception {
    return mockMvc.perform(addRequestBodyAndToken(put(path), /*user=*/null, body));
  }

  protected ResultActions performPutRequest(User user, String path, String body) throws Exception {
    return mockMvc.perform(addRequestBodyAndToken(put(path), user, body));
  }

  protected ResultActions performDeleteRequest(User user, String path) throws Exception {
    return mockMvc.perform(addRequestBodyAndToken(delete(path), user, /*body=*/null));
  }

  private RequestBuilder addRequestBodyAndToken(
      MockHttpServletRequestBuilder requestBuilder,
      User user,
      String body) throws UnsupportedEncodingException {
    if (user != null) {
      requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + getAuthenticationToken(user));
    }

    if (body != null) {
      requestBuilder.contentType(MediaType.APPLICATION_JSON);
      requestBuilder.content(body);
    }

    return requestBuilder;
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
}
