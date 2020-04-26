package com.azkar.payload.challengecontroller.responses.utils;

import com.azkar.entities.Challenge;
import com.azkar.entities.User.UserChallenge;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class UserReturnedChallenge {
  Challenge challengeInfo;
  UserChallenge userStatus;
}
