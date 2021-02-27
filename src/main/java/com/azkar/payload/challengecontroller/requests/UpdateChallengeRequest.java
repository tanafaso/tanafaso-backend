package com.azkar.payload.challengecontroller.requests;

import com.azkar.entities.Challenge;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UpdateChallengeRequest {

  Challenge newChallenge;
}
