package com.azkar.payload.challengecontroller.requests;

import com.azkar.entities.Challenge;
import com.azkar.payload.exceptions.BadRequestException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AddPersonalChallengeRequest extends AddChallengeRequest {

  @Builder(builderMethodName = "addPersonalChallengeRequestBuilder")
  public AddPersonalChallengeRequest(Challenge challenge) {
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
