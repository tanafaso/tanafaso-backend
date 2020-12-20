package com.azkar.payload.challengecontroller.responses;

import com.azkar.payload.ResponseBase;
import com.azkar.payload.challengecontroller.responses.GetChallengesResponse.UserChallenge;
import com.google.common.annotations.VisibleForTesting;

public class GetChallengeResponse extends ResponseBase<UserChallenge> {

  @VisibleForTesting
  public static final String CHALLENGE_NOT_FOUND_ERROR =
      "Could not find the challenge you requested.";
}
