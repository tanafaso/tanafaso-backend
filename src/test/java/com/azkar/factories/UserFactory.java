package com.azkar.factories;

import com.azkar.entities.User;
import org.bson.types.ObjectId;

public class UserFactory {

  static int usersRequested = 0;

  public static User getNewUser() {
    usersRequested++;
    return User.builder()
        .id(new ObjectId().toString())
        .email("testEmail" + usersRequested)
        .username("testUsername" + usersRequested)
        .name("testName" + usersRequested)
        .build();
  }
}
