package com.azkar.payload;

import com.azkar.payload.exceptions.BadRequestException;

public interface RequestBodyBase {

  public void validate() throws BadRequestException;
}
