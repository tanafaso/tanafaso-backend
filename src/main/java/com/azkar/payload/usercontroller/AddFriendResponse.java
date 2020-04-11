package com.azkar.payload.usercontroller;

import com.azkar.entities.Friendship;
import com.azkar.payload.ResponseBase;

public class AddFriendResponse extends ResponseBase<Friendship> {

  public static final String ERROR_USER_NOT_FOUND = "User not found.";
  public static final String ERROR_FRIENDSHIP_ALREADY_REQUESTED =
      "Already requested friendship to this user.";
}
