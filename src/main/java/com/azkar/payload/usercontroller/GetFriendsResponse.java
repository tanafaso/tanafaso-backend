package com.azkar.payload.usercontroller;

import com.azkar.payload.ResponseBase;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
public class GetFriendsResponse extends ResponseBase {

  private List<FriendshipStatus> friendshipStatusList;

  @Builder
  public static class FriendshipStatus {

    private String userId;
    private boolean isPending;
  }
}
