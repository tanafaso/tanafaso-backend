package com.azkar.payload.challengecontroller.requests;

import com.azkar.entities.Challenge;
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
public class AddChallengeRequest extends RequestBodyBase {

  @VisibleForTesting
  public static final String PAST_EXPIRY_DATE_ERROR = "Expiry date is in the past.";
  @VisibleForTesting
  public static final String GROUP_NOT_FOUND_ERROR = "The given group is not found.";

//  private List<SubChallenges> subChallenges;
  private Challenge challenge;

  @Override
  public void validate() throws BadRequestException {
//    checkNotNull(motivation, name, subChallenges, groupId);
//    if (expiryDate < Instant.now().getEpochSecond()) {
//      throw new BadRequestException(PAST_EXPIRY_DATE_ERROR);
//    }
  }
}
