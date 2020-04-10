package com.azkar.configs.authentication;

public class UsernameGenerationException extends Exception {

  @Override public String getMessage() {
    return "Can not generate unique username";
  }
}
