package com.azkar;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.azkar.entities.User;
import com.azkar.repos.UserRepo;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public abstract class ControllerTestBase {

  @Autowired
  MockMvc mockMvc;

  // TODO(issue#41): Use different database instance in test environment.
  @Autowired
  UserRepo userRepo;

  // TODO(issue#40): Use different jwt secret in test environment.
  @Value("${app.jwtSecret}")
  String jwtSecret;

  @Autowired
  MongoTemplate mongoTemplate;
  private String currentUserToken;

  @After
  public final void afterBase() {
    mongoTemplate.getDb().drop();
  }

  protected void authenticate(User user) {
    try {
      final long TOKEN_TIMEOUT_IN_MILLIS = TimeUnit.MINUTES.toMillis(1);
      currentUserToken =
          JWT.create()
              .withSubject(user.getId())
              .withExpiresAt(new Date(System.currentTimeMillis() + TOKEN_TIMEOUT_IN_MILLIS))
              .sign(Algorithm.HMAC512(jwtSecret));
      userRepo.save(user);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }

  protected ResultActions prepareGetRequest(String path) throws Exception {
    assertThat(currentUserToken, notNullValue());

    return mockMvc
        .perform(get(path)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + currentUserToken));
  }

  protected String mapToJson(Object obj) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.writeValueAsString(obj);
  }
}
