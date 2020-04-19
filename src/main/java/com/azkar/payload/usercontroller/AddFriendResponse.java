package com.azkar.payload.usercontroller;

import com.azkar.payload.ResponseBase;

public class AddFriendResponse extends ResponseBase {

  public static final String USER_NOT_FOUND_ERROR = "User not found.";
  public static final String FRIENDSHIP_ALREADY_REQUESTED_ERROR =
      "Already requested friendship to this user.";
}