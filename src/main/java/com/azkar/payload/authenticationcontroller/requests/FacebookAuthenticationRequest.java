package com.azkar.payload.authenticationcontroller.requests;

import com.azkar.payload.RequestBodyBase;
import com.azkar.payload.exceptions.BadRequestException;
import lombok.Data;

@Data
public class FacebookAuthenticationRequest extends RequestBodyBase {

  private String token;
  private String facebookUserId;

  @Override public void validate() throws BadRequestException {
    checkNotNull(token, facebookUserId);
  }
}
