package com.azkar.payload.authenticationcontroller.requests;

import com.azkar.payload.RequestBodyBase;
import com.azkar.payload.exceptions.BadRequestException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePasswordRequest extends RequestBodyBase {

  String password;

  @Override
  public void validate() throws BadRequestException {
    checkNotNull(password);
    EmailAuthenticationRequestBodyUtil.validatePassword(password);
  }
}
