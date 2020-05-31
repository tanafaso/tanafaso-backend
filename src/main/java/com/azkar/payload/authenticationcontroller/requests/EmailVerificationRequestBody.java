package com.azkar.payload.authenticationcontroller.requests;

import com.azkar.payload.RequestBodyBase;
import com.azkar.payload.exceptions.BadRequestException;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailVerificationRequestBody extends RequestBodyBase {

  private String email;
  private Integer pin;

  @Override public void validate() throws BadRequestException {
    checkNotNull(email, pin);
  }
}
