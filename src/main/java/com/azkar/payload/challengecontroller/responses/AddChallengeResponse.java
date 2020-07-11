package com.azkar.payload.challengecontroller.responses;

import com.azkar.entities.Challenge;
import com.azkar.payload.ResponseBase;

public class AddChallengeResponse extends ResponseBase<Challenge> {

  public static final String GROUP_NOT_FOUND_ERROR = "The given group is not found.";
  public static final String NOT_GROUP_MEMBER_ERROR =
      "The user is not a member of the given group.";
}
