package com.azkar.payload.usercontroller.responses;

import com.azkar.payload.ResponseBase;
import com.azkar.payload.usercontroller.responses.GetFriendsLeaderboardResponse.FriendshipScores;
import com.azkar.payload.utils.UserScore;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

public class GetFriendsLeaderboardResponse extends ResponseBase<List<FriendshipScores>> {

  @Builder
  @Getter
  public static class FriendshipScores {

    UserScore currentUserScore;
    UserScore friendScore;
  }
}
