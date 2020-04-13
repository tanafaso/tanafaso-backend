package com.azkar.payload.challengecontroller;

import com.azkar.entities.Challenge.SubChallenges;
import com.azkar.payload.RequestBodyBase;
import com.azkar.payload.exceptions.BadRequestException;
import java.time.Instant;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddPersonalChallengeRequest implements RequestBodyBase {

  private static final String PAST_EXPIRY_DATE_ERROR = "Expiry date is in the past.";

  private String motivation;
  private String name;
  private long expiryDate;
  private List<SubChallenges> subChallenges;


  @Override
  public void validate() throws BadRequestException {
    if (motivation == null || name == null || subChallenges == null) {
      throw new BadRequestException(BadRequestException.REQUIRED_FIELDS_NOT_GIVEN);
    }
    if (expiryDate <= Instant.now().getEpochSecond()) {
      throw new BadRequestException(PAST_EXPIRY_DATE_ERROR);
    }
  }
}
