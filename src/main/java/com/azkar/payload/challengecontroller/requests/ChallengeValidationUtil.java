package com.azkar.payload.challengecontroller.requests;

import com.azkar.entities.challenges.Challenge;
import com.azkar.payload.RequestBodyBase;
import com.azkar.payload.ResponseBase.Status;
import com.azkar.payload.exceptions.BadRequestException;
import java.time.Instant;

public class ChallengeValidationUtil {

  public static void validate(Challenge challenge) {
    RequestBodyBase.checkNotNull(challenge.getGroupId());

    if (challenge.getExpiryDate() < Instant.now().getEpochSecond()) {
      throw new BadRequestException(new Status(Status.PAST_EXPIRY_DATE_ERROR));
    }
  }
}
