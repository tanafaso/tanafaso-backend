package com.azkar.payload.challengecontroller.responses;

import com.azkar.entities.Challenge;
import com.azkar.entities.User.UserChallengeStatus;
import com.azkar.payload.ResponseBase;
import com.azkar.payload.challengecontroller.responses.GetChallengesResponse.UserReturnedChallenge;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class GetChallengesResponse extends ResponseBase<List<UserReturnedChallenge>> {

  @Builder(toBuilder = true)
  @NoArgsConstructor
  @AllArgsConstructor
  @Setter
  @Getter
  public static class UserReturnedChallenge {

    Challenge challengeInfo;
    UserChallengeStatus userChallengeStatus;
  }
}
