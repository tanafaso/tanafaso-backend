package com.azkar.payload.authenticationcontroller.responses;

import com.azkar.payload.ResponseBase;

public class EmailVerificationResponse extends ResponseBase {

  public static final String EMAIL_ALREADY_VERIFIED_ERROR =
      "This email has already been verified.";
  public static final String VERIFICATION_ERROR = "This PIN is incorrect.";
}
