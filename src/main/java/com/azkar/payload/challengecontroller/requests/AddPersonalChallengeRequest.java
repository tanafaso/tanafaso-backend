package com.azkar.payload.challengecontroller.requests;

import com.azkar.entities.challenges.AzkarChallenge;
import com.azkar.payload.exceptions.BadRequestException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Deprecated
@Getter
@AllArgsConstructor
public class AddPersonalChallengeRequest extends AddChallengeRequest {

  @Builder(builderMethodName = "addPersonalChallengeRequestBuilder")
  public AddPersonalChallengeRequest(AzkarChallenge challenge) {
    this.challenge = challenge;
  }

  @Override
  public void validate() throws BadRequestException {
    checkNotNull(
        challenge.getName(),
        challenge.getSubChallenges());

    validateExpiryDate();
    validateSubChallenges();
  }
}
