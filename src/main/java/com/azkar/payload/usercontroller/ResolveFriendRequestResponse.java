package com.azkar.payload.usercontroller;

import com.azkar.payload.ResponseBase;

public class ResolveFriendRequestResponse extends ResponseBase {

  public static final String NO_FRIEND_REQUEST_EXIST_ERROR =
      "No friend request is pending between these two users.";
  public static final String FRIEND_REQUEST_ALREADY_ACCEPTED_ERROR =
      "The friend request is already accepted.";
}
