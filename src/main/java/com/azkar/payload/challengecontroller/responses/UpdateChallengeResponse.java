package com.azkar.payload.challengecontroller.responses;

import com.azkar.payload.ResponseBase;

public class UpdateChallengeResponse extends ResponseBase<String> {

  public static final String INCREMENTING_LEFT_REPETITIONS_ERROR =
      "The value of left repetitions can not be incremented.";
  public static final String NON_EXISTENT_SUB_CHALLENGE_ERROR =
      "One or more of the sub challenges to be updated do not exist.";
  public static final String CHALLENGE_NOT_FOUND_ERROR = "Could not find the challenge to update.";
  public static final String MISSING_OR_DUPLICATED_SUB_CHALLENGE_ERROR =
      "There is a missing or duplicated sub challenge.";
  public static final String CHALLENGE_EXPIRED_ERROR = "Can't perform expired challenges.";
}
