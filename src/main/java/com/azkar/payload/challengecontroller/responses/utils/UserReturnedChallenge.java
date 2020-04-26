package com.azkar.payload.challengecontroller.responses.utils;

import com.azkar.entities.Challenge;
import com.azkar.entities.User.UserChallenge;
import lombok.Builder;
import lombok.Getter;

@Builder(toBuilder = true)
@Getter
public class UserReturnedChallenge {
  Challenge challengeInfo;
  UserChallenge userStatus;
}
