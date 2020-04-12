package com.azkar.payload.challengecontroller;

import com.azkar.entities.Challenge.Subchallenges;
import com.azkar.payload.RequestBodyBase;
import com.azkar.payload.exceptions.BadRequestException;
import java.time.Instant;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddPersonalChallengeRequest implements RequestBodyBase {

  private static final String PAST_EXPIRY_DATE = "Expiry date is in the past.";

  String motivation;
  String name;
  long expiryDate;
  List<Subchallenges> subChallenges;


  @Override
  public void validate() throws BadRequestException {
    if (motivation == null || name == null || subChallenges == null) {
      throw new BadRequestException(BadRequestException.REQUIRED_FIELDS_NOT_GIVEN);
    }
    if (expiryDate <= Instant.now().getEpochSecond()) {
      throw new BadRequestException(PAST_EXPIRY_DATE);
    }
  }
}
