package com.azkar.payload.usercontroller;

import com.azkar.payload.ResponseBase;

public class ResolveFriendRequestResponse extends ResponseBase {

  public static final String kFriendshipNotFoundError =
      "No friend request is pending between these two users";
  public static final String kFriendshipNotPendingError = "Users are already friends.";
}
