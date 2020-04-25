package com.azkar.payload.challengecontroller.requests;

import com.azkar.entities.Challenge.SubChallenges;
import com.azkar.payload.RequestBodyBase;
import com.azkar.payload.exceptions.BadRequestException;
import com.azkar.repos.GroupRepo;
import com.google.common.annotations.VisibleForTesting;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
public class AddChallengeRequest extends RequestBodyBase {

  @VisibleForTesting
  public static final String PAST_EXPIRY_DATE_ERROR = "Expiry date is in the past.";
  @VisibleForTesting
  public static String GROUP_NOT_FOUND_ERROR = "The given group is not found.";

  @Autowired
  GroupRepo groupRepo;

  private String motivation;
  private String name;
  private long expiryDate;
  private List<SubChallenges> subChallenges;
  private String groupId;

  @Override
  public void validate() throws BadRequestException {
    if (anyNull(motivation, name, subChallenges, groupId)) {
      throw new BadRequestException(BadRequestException.REQUIRED_FIELDS_NOT_GIVEN_ERROR);
    }

    if (expiryDate < Instant.now().getEpochSecond()) {
      throw new BadRequestException(PAST_EXPIRY_DATE_ERROR);
    }
  }
}
