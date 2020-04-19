package com.azkar.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.azkar.entities.User;
import com.azkar.repos.UserRepo;
import com.azkar.services.UserService;
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
import org.springframework.test.web.servlet.ResultActions;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ControllerTestBase {

  @Autowired
  MockMvc mockMvc;

  @Autowired
  UserRepo userRepo;

  @Autowired
  UserService userService;

  @Autowired
  MongoTemplate mongoTemplate;

  @Value("${app.jwtSecret}")
  String jwtSecret;

  @After
  public final void afterBase() {
    mongoTemplate.getDb().drop();
  }

  protected void addNewUser(User user) {
    userService.addNewUser(user);
  }

  private String getAuthenticationToken(User user) throws UnsupportedEncodingException {
    final long TOKEN_TIMEOUT_IN_MILLIS = TimeUnit.MINUTES.toMillis(1);
    return JWT.create()
        .withSubject(user.getId())
        .withExpiresAt(new Date(System.currentTimeMillis() + TOKEN_TIMEOUT_IN_MILLIS))
        .sign(Algorithm.HMAC512(jwtSecret));
  }

  protected ResultActions performGetRequest(User user, String path) throws Exception {
    return mockMvc
        .perform(get(path)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAuthenticationToken(user)));
  }

  protected ResultActions performPostRequest(User user, String path, String body) throws Exception {
    if (body == null) {
      return mockMvc
          .perform(post(path)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAuthenticationToken(user)));
    }
    return mockMvc
        .perform(post(path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAuthenticationToken(user)));
  }

  protected ResultActions performPutRequest(User user, String path, String body) throws Exception {
    if (body == null) {
      return mockMvc
          .perform(put(path)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAuthenticationToken(user)));
    }

    return mockMvc
        .perform(put(path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAuthenticationToken(user)));
  }

  protected ResultActions performDeleteRequest(User user, String path) throws Exception {
    return mockMvc
        .perform(delete(path)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAuthenticationToken(user)));
  }

  protected String mapToJson(Object obj) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
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
