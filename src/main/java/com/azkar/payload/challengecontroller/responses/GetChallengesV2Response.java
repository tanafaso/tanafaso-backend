package com.azkar.payload.challengecontroller.responses;

import com.azkar.entities.challenges.AzkarChallenge;
import com.azkar.entities.challenges.MeaningChallenge;
import com.azkar.payload.ResponseBase;
import com.azkar.payload.challengecontroller.responses.GetChallengesV2Response.Challenge;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class GetChallengesV2Response extends ResponseBase<List<Challenge>> {

  // This class holds an instance of one of the challenge types.
  @Builder
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Setter
  public static class Challenge implements Comparable<Challenge> {

    AzkarChallenge azkarChallenge;
    MeaningChallenge meaningChallenge;

    private boolean unFinishedAndUnExpired() {
      return azkarChallenge != null ? !azkarChallenge.finished() && !azkarChallenge.expired() :
          !meaningChallenge.isFinished() && !meaningChallenge.expired();
    }

    // Sorts such that unfinished challenges are first and resolved ties in the descending order of
    // expiry date.
    @Override public int compareTo(Challenge o) {
      if (unFinishedAndUnExpired() && !o.unFinishedAndUnExpired()) {
        return -1;
      }

      if (!unFinishedAndUnExpired() && o.unFinishedAndUnExpired()) {
        return 1;
      }

      long firstExpiryDate = azkarChallenge != null ? azkarChallenge.getExpiryDate()
          : meaningChallenge.getExpiryDate();
      long secondExpiryDate = o.azkarChallenge != null ? o.azkarChallenge.getExpiryDate()
          : o.meaningChallenge.getExpiryDate();
      return Long.compare(firstExpiryDate, secondExpiryDate);
    }
  }
}
