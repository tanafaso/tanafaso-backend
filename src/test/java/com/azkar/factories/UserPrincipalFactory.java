package com.azkar.factories;

import com.azkar.configs.authentication.UserPrincipal;

public class UserPrincipalFactory {

  public static UserPrincipal getUserPrincipal() {
    UserPrincipal userPrincipal = new UserPrincipal();
    userPrincipal.setId("testId");
    userPrincipal.setUsername("testUsername");
    return userPrincipal;
  }
}
