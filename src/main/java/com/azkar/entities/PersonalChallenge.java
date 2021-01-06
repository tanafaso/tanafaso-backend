package com.azkar.entities;

import com.azkar.entities.User.SubChallengeProgress;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

public class PersonalChallenge {

  @Delegate
  @Getter
  private Challenge challenge;
  @Setter
  @Getter
  private List<SubChallengeProgress> subChallengesProgress;

  private PersonalChallenge(Challenge challenge) {
    this.challenge = challenge;
    subChallengesProgress = SubChallengeProgress
        .fromSubChallengesCollection(challenge.getSubChallenges());
  }

  public static PersonalChallenge getInstance(Challenge challenge) {
    return new PersonalChallenge(challenge);
  }

}
