package com.azkar.payload.groupcontroller.responses;

import com.azkar.payload.ResponseBase;
import com.azkar.payload.groupcontroller.responses.GetGroupLeaderboardResponse.UserScore;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

public class GetGroupLeaderboardResponse extends ResponseBase<List<UserScore>> {

  public static final String NOT_MEMBER_IN_GROUP_ERROR = "Not a member in this group";

  @Builder
  @Getter
  public static class UserScore {

    String firstName;
    String lastName;
    String username;
    int totalScore;
  }
}
