package com.azkar.controllers.utils;

import com.azkar.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.ResultActions;

@Service
public class AzkarApi {

  @Autowired
  HttpClient httpClient;

  public ResultActions getProfile(User user) throws Exception {
    return httpClient.performGetRequest(user, "/users/me");
  }

  public ResultActions getProfileWithoutAuthentication() throws Exception {
    return httpClient.performGetRequest(/*user=*/ (User) null, "/users/me");
  }
}
