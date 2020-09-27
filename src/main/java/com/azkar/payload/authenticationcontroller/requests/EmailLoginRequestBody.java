package com.azkar.payload.authenticationcontroller.requests;

import com.azkar.payload.RequestBodyBase;
import com.azkar.payload.exceptions.BadRequestException;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class EmailLoginRequestBody extends RequestBodyBase {

  private String email;
  private String password;

  @Override public void validate() throws BadRequestException {
    checkNotNull(email, password);
    EmailAuthenticationRequestBodyUtil.validateEmail(email);
    EmailAuthenticationRequestBodyUtil.validatePassword(password);
  }
}
