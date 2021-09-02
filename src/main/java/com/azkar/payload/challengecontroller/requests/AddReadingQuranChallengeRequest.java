package com.azkar.payload.challengecontroller.requests;

import com.azkar.entities.challenges.ReadingQuranChallenge;
import com.azkar.entities.challenges.ReadingQuranChallenge.SurahSubChallenge;
import com.azkar.payload.RequestBodyBase;
import com.azkar.payload.ResponseBase.Status;
import com.azkar.payload.exceptions.BadRequestException;
import java.util.HashSet;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AddReadingQuranChallengeRequest extends RequestBodyBase {

  protected ReadingQuranChallenge challenge;
  private List<String> friendsIds;

  @Builder(builderMethodName = "AddReadingQuranChallengeRequestBuilder")
  public AddReadingQuranChallengeRequest(List<String> friendsIds, ReadingQuranChallenge challenge) {
    this.challenge = challenge;
    this.friendsIds = friendsIds;
  }

  @Override
  public void validate() throws BadRequestException {
    ChallengeValidationUtil.validateExpiryDate(challenge.getExpiryDate());

    validateFriendIds();
    validateSubChallenges();
  }

  private void validateFriendIds() {
    if (new HashSet<>(friendsIds).size() != friendsIds.size()) {
      throw new BadRequestException(new Status(Status.DUPLICATE_FRIEND_IDS_PROVIDED_ERROR));
    }
  }

  protected void validateSubChallenges() {
    challenge.getSurahSubChallenges().forEach(subChallenges -> {
      if (subChallenges.getStartingVerseNumber() > subChallenges.getEndingVerseNumber()) {
        throw new BadRequestException(new Status(Status.STARTING_VERSE_AFTER_ENDING_VERSE_ERROR));
      }
    });
  }
}
