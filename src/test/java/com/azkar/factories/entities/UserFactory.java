package com.azkar.factories.entities;

import com.azkar.entities.User;
import com.azkar.entities.User.UserFacebookData;
import com.azkar.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;

public class UserFactory {

  @Autowired
  static UserService userService;
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
        .email("testFacebookEmail" + usersRequested + "@example_domain.com")
        .userId("testFacebookId" + usersRequested)
        .name("testFacebookName" + usersRequested)
        .build();

    return User.builder()
        .id("testId" + usersRequested)
        .userFacebookData(userFacebookData)
        .build();
  }
}
