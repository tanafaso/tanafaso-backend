package com.azkar.payload.challengecontroller.responses;

import com.azkar.entities.Challenge;
import com.azkar.entities.User.UserChallengeStatus;
import com.azkar.payload.ResponseBase;
import com.azkar.payload.challengecontroller.responses.GetChallengesResponse.UserChallenge;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class GetChallengesResponse extends ResponseBase<List<UserChallenge>> {

  public static final String GROUP_NOT_FOUND_ERROR = "Group not found.";
  public static final String NON_GROUP_MEMBER_ERROR = "The user is not a member of this group.";

  @Builder(toBuilder = true)
  @NoArgsConstructor
  @AllArgsConstructor
  @Setter
  @Getter
  public static class UserChallenge {

    // NOTE: challengeInfo.id and userChallengeStatus.challengeId must be equal
    // as they represent the same challenge.
    Challenge challengeInfo;
    UserChallengeStatus userChallengeStatus;
  }
}
