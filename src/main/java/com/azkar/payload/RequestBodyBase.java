package com.azkar.payload;

import com.azkar.payload.exceptions.BadRequestException;

public interface RequestBodyBase {

  void validate() throws BadRequestException;
}
