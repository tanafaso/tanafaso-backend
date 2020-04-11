package com.azkar.payload.challengecontroller;

import com.azkar.entities.Challenge;
import com.azkar.payload.ResponseBase;
import com.azkar.payload.ResponseBase.Error;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GetChallengeResponse {
  Challenge data;
  Error error;
}
