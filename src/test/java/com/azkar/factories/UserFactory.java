package com.azkar.factories;

import com.azkar.entities.User;

public class UserFactory {

  static int usersRequested = 0;

  public static User getUser() {
    usersRequested++;
    return User.builder()
        .id("testId" + usersRequested)
        .email("testEmail" + usersRequested)
        .username("testUsername" + usersRequested)
        .name("testName" + usersRequested)
        .build();
  }
}
