package com.azkar.payload.challengecontroller.responses;

import com.azkar.entities.Challenge;
import com.azkar.payload.ResponseBase;
import com.google.common.annotations.VisibleForTesting;

public class GetChallengeResponse extends ResponseBase<Challenge> {

  @VisibleForTesting
  public static final String CHALLENGE_NOT_FOUND_ERROR =
      "Could not find the challenge you requested.";
}
