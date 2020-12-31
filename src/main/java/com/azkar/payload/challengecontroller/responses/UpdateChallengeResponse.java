package com.azkar.payload.challengecontroller.responses;

import com.azkar.payload.ResponseBase;

public class UpdateChallengeResponse extends ResponseBase<String> {

  public static final String INCREMENTING_LEFT_REPETITIONS_ERROR =
      "The value of left repetitions can not be incremented.";
  public static final String NON_EXISTENT_SUB_CHALLENGE_ERROR =
      "One or more of the sub challenges to be updated do not exist.";
}
