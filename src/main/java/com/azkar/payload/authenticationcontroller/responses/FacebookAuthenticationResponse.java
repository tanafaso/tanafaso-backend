package com.azkar.payload.authenticationcontroller.responses;

import com.azkar.payload.ResponseBase;

// The response of connection with facebook will be either an error message or the JWT token
// should be sent in the headers.
public class FacebookAuthenticationResponse extends ResponseBase {

  public static final String AUTHENTICATION_WITH_FACEBOOK_ERROR =
      "Something wrong happened while authenticating with facebook, please try again.";

  public static final String SOMEONE_ELSE_ALREADY_CONNECTED_ERROR =
      "Someone else already connected to facebook using this account.";
}
