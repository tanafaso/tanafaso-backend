package com.azkar.payload;

import com.azkar.payload.exceptions.BadRequestException;
import java.util.Arrays;
import java.util.Objects;

public abstract class RequestBodyBase {

  protected static void checkNotNull(Object... arguments) throws BadRequestException {
    if (Arrays.stream(arguments).anyMatch(Objects::isNull)) {
      throw new BadRequestException(BadRequestException.REQUIRED_FIELDS_NOT_GIVEN_ERROR);
    }
  }

  public abstract void validate() throws BadRequestException;
}
