package com.azkar.payload.authenticationcontroller.requests;

import com.azkar.payload.RequestBodyBase;
import com.azkar.payload.exceptions.BadRequestException;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class EmailRegistrationRequestBody extends RequestBodyBase {

  private String email;
  private String password;
  private String firstName;
  private String lastName;

  @Override public void validate() throws BadRequestException {
    checkNotNull(email, password, firstName, lastName);
    EmailAuthenticationRequestBodyUtil.validateEmail(email);
    EmailAuthenticationRequestBodyUtil.validatePassword(password);
    EmailAuthenticationRequestBodyUtil.validateName(firstName);
    EmailAuthenticationRequestBodyUtil.validateName(lastName);
  }

}
