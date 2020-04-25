package com.azkar.payload;

import com.azkar.payload.exceptions.BadRequestException;
import java.util.Arrays;
import java.util.Objects;

public abstract class RequestBodyBase {

  public abstract void validate() throws BadRequestException;

  protected static boolean anyNull(Object... arguments) {
    return Arrays.stream(arguments).anyMatch(Objects::isNull);
  }
}
