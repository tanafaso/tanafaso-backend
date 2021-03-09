package com.azkar.payload.challengecontroller.requests;

import com.azkar.entities.Challenge;
import com.azkar.payload.RequestBodyBase;
import com.azkar.payload.exceptions.BadRequestException;
import com.google.common.annotations.VisibleForTesting;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AddChallengeRequest extends RequestBodyBase {

  @VisibleForTesting
  public static final String PAST_EXPIRY_DATE_ERROR = "Expiry date is in the past.";
  @VisibleForTesting
  public static final String MALFORMED_SUB_CHALLENGES_ERROR =
      "Sub challenges repetitions must be greater than 0.";

  protected Challenge challenge;

  @Override
  public void validate() throws BadRequestException {
    checkNotNull(
        challenge.getName(),
        challenge.getSubChallenges(),
        challenge.getGroupId());

    validateExpiryDate();
    validateSubChallenges();
  }

  protected void validateExpiryDate() {
    if (challenge.getExpiryDate() < Instant.now().getEpochSecond()) {
      throw new BadRequestException(PAST_EXPIRY_DATE_ERROR);
    }
  }

  protected void validateSubChallenges() {
    challenge.getSubChallenges().forEach(subChallenges -> {
      if (subChallenges.getRepetitions() <= 0) {
        throw new BadRequestException(MALFORMED_SUB_CHALLENGES_ERROR);
      }
    });
  }
}
