package com.azkar.factories.controllers;

import com.azkar.entities.User;
import com.azkar.entities.User.UserFacebookData;

public class UserFactory {

  static int usersRequested = 0;

  public static User getNewUser() {
    usersRequested++;
    return User.builder()
        .id("testId" + usersRequested)
        .email("testEmail" + usersRequested + "@example_domain.com")
        .username("testUsername" + usersRequested)
        .name("testName" + usersRequested)
        .build();
  }

  public static User getUserRegisteredWithFacebook() {
    usersRequested++;
    UserFacebookData userFacebookData = UserFacebookData.builder()
        .email("testEmail" + usersRequested + "@example_domain.com")
        .userId("testFacebookId" + usersRequested)
        .name("testName" + usersRequested)
        .build();

    return User.builder()
        .id("testId" + usersRequested)
        .userFacebookData(userFacebookData)
        .build();
  }
}
