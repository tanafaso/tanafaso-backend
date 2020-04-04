package com.azkar.controllers.responses;

import java.util.List;

public class GetFriendsResponse {

  private List<Friend> friendsList;

  public List<Friend> getFriendsList() {
    return friendsList;
  }

  public void setFriendsList(
      List<Friend> friendsList) {
    this.friendsList = friendsList;
  }

  public static class Friend {

    String userId;
    boolean isPending;

    public Friend(String userId, boolean isPending) {
      this.userId = userId;
      this.isPending = isPending;
    }
  }
}
