package com.azkar.payload.authenticationcontroller.responses;

import com.azkar.payload.ResponseBase;

public class EmailLoginResponse extends ResponseBase {

  public static final String USER_ALREADY_LOGGED_IN_ERROR = "The user is already logged in.";
  public static final String EMAIL_PASSWORD_COMBINATION_ERROR =
      "Email and password combination is incorrect.";
  public static final String LOGIN_WITH_EMAIL_ERROR =
      "Something wrong happened while logging in, please try again.";
  public static final String EMAIL_NOT_VERIFIED_ERROR =
      "Please verify your email first and try logging in again.";

}
