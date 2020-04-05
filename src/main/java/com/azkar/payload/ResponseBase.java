package com.azkar.payload;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
abstract public class ResponseBase<T> {
  T data;
  Error error;

  static class Error {
    private final String message;

    public Error(String message) {
      this.message = message;
    }

  }
}
