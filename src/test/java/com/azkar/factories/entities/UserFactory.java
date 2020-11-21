package com.azkar.factories.entities;

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

  public static User getNewUserWithEmailAndEncodedPassword(String email, String encodedPassword) {
    usersRequested++;
    return User.builder()
        .id("testId" + usersRequested)
        .email(email)
        .encodedPassword(encodedPassword)
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
        .name(userFacebookData.getName())
        .build();
  }
}
