package com.azkar.payload.exceptions;

import com.azkar.payload.ResponseBase.Error;

public class BadRequestException extends RuntimeException {

  public Error error;

  public BadRequestException(Error error) {
    super("Bad Request");
    this.error = error;
  }
}
