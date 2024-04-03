package com.azkar.payload.challengecontroller.responses;

import com.azkar.entities.challenges.AzkarChallenge;
import com.azkar.entities.challenges.CustomSimpleChallenge;
import com.azkar.entities.challenges.GlobalChallenge;
import com.azkar.entities.challenges.MeaningChallenge;
import com.azkar.entities.challenges.MemorizationChallenge;
import com.azkar.entities.challenges.ReadingQuranChallenge;
import com.azkar.payload.ResponseBase;
import com.azkar.payload.challengecontroller.responses.GetGlobalChallengeResponse.ReturnedChallenge;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class GetGlobalChallengeResponse extends ResponseBase<ReturnedChallenge> {
  // This class holds an instance of one of the challenge types. For now, it can only hold an
  // AzkarChallenge instance.
  @Builder
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Setter
  public static class ReturnedChallenge {
    AzkarChallenge azkarChallenge;
    int finishedCount;
  }
}
