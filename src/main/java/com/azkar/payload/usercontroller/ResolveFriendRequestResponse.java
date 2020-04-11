package com.azkar.payload.usercontroller;

import com.azkar.payload.ResponseBase;

public class ResolveFriendRequestResponse extends ResponseBase {

  public static final String ERROR_NO_FRIEND_REQUEST_EXIST =
      "No friend request is pending between these two users.";
  public static final String ERROR_FRIEND_REQUEST_ALREADY_ACCEPTED =
      "The friend request is already accepted.";
}
