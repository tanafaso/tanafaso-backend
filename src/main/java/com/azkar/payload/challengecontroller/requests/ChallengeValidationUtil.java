package com.azkar.payload.challengecontroller.requests;

import com.azkar.entities.challenges.ChallengeBase;
import com.azkar.payload.RequestBodyBase;
import com.azkar.payload.ResponseBase.Status;
import com.azkar.payload.exceptions.BadRequestException;
import java.time.Instant;

public class ChallengeValidationUtil {

  public static void validate(ChallengeBase challenge) {
    if (challenge.getExpiryDate() < Instant.now().getEpochSecond()) {
      throw new BadRequestException(new Status(Status.PAST_EXPIRY_DATE_ERROR));
    }
  }
}
