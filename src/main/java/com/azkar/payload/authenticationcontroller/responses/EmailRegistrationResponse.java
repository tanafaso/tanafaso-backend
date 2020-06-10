package com.azkar.payload.authenticationcontroller.responses;

import com.azkar.entities.RegistrationEmailConfirmationState;
import com.azkar.payload.ResponseBase;

public class EmailRegistrationResponse extends ResponseBase<RegistrationEmailConfirmationState> {

  public static final String USER_ALREADY_REGISTERED_ERROR = "This user is already registered.";
  public static final String USER_ALREADY_REGISTERED_WITH_FACEBOOK =
      "The user is already registered with facebook. Please try logging in using facebook.";
  public static final String PIN_ALREADY_SENT_TO_USER_ERROR =
      "A pin has already been sent to the user";
}
