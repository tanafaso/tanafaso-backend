package com.azkar.payload.groupcontroller.responses;

import com.azkar.payload.ResponseBase;
import com.azkar.payload.groupcontroller.responses.GetGroupLeaderboardResponse.UserScore;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

public class GetGroupLeaderboardResponse extends ResponseBase<List<UserScore>> {

  @Builder
  @Getter
  public static class UserScore {

    String firstName;
    String lastName;
    String username;
    int totalScore;
  }
}
