package com.azkar.payload.challengecontroller.requests;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UpdateChallengeRequest {

  List<ModifiedSubChallenge> allModifiedSubChallenges;


  @Getter
  @Builder(toBuilder = true)
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ModifiedSubChallenge {

    String zekrId;
    int newLeftRepetitions;
  }
}
