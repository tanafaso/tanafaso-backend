package com.azkar.payload.challengecontroller;

import com.azkar.entities.Challenge;
import com.azkar.payload.ResponseBase;

public class AddPersonalChallengeResponse extends ResponseBase<Challenge> {

  public static String USER_NOT_LOGGED_IN = "Cannot find logged in user.";
}
