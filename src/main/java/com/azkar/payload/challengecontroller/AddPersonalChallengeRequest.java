package com.azkar.payload.challengecontroller;

import com.azkar.entities.Challenge.SubChallenges;
import com.azkar.payload.RequestBodyBase;
import com.azkar.payload.exceptions.BadRequestException;
import com.google.common.annotations.VisibleForTesting;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
public class AddPersonalChallengeRequest implements RequestBodyBase {

  @VisibleForTesting
  public static final String PAST_EXPIRY_DATE_ERROR = "Expiry date is in the past.";

  private String motivation;
  private String name;
  private long expiryDate;
  private List<SubChallenges> subChallenges;


  @Override
  public void validate() throws BadRequestException {
    if (motivation == null || name == null || subChallenges == null) {
      throw new BadRequestException(BadRequestException.REQUIRED_FIELDS_NOT_GIVEN_ERROR);
    }
    if (expiryDate <= Instant.now().getEpochSecond()) {
      throw new BadRequestException(PAST_EXPIRY_DATE_ERROR);
    }
  }
}
