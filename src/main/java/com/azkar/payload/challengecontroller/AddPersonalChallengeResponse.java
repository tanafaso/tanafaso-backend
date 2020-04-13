package com.azkar.payload.challengecontroller;

import com.azkar.entities.Challenge;
import com.azkar.payload.ResponseBase;

public class AddPersonalChallengeResponse extends ResponseBase<Challenge> {

  public static final String USER_NOT_LOGGED_IN_ERROR = "Cannot find logged in user.";
}
