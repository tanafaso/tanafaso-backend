package com.azkar.payload.challengecontroller.responses;

import com.azkar.entities.challenges.AzkarChallenge;
import com.azkar.entities.challenges.MeaningChallenge;
import com.azkar.payload.ResponseBase;
import com.azkar.payload.challengecontroller.responses.GetChallengesV2Response.ReturnedChallenge;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class GetChallengesV2Response extends ResponseBase<List<ReturnedChallenge>> {

  // This class holds an instance of one of the challenge types.
  @Builder
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Setter
  public static class ReturnedChallenge {

    AzkarChallenge azkarChallenge;
    MeaningChallenge meaningChallenge;
  }
}
