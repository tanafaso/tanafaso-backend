package com.azkar.entities;

import com.azkar.entities.User.UserSubChallenge;
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
  private List<UserSubChallenge> userSubChallenges;

  private PersonalChallenge(Challenge challenge) {
    this.challenge = challenge;
    userSubChallenges = UserSubChallenge.fromSubChallengesCollection(challenge.getSubChallenges());
  }

  public static PersonalChallenge getInstance(Challenge challenge) {
    return new PersonalChallenge(challenge);
  }

}
