package com.azkar.payload.exceptions;

public class BadRequestException extends RuntimeException {

  public static final String REQUIRED_FIELDS_NOT_GIVEN_ERROR = "Some required fields are not provided.";

  public BadRequestException(String message) {
    super(message);
  }
}
