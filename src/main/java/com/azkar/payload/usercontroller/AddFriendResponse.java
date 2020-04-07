package com.azkar.payload.usercontroller;

import com.azkar.entities.Friendship;
import com.azkar.payload.ResponseBase;

public class AddFriendResponse extends ResponseBase<Friendship> {

  public static final String kUserNotFoundError = "User not found.";
  public static final String kFriendshipAlreadyRequestedError =
      "Already requested friendship to this user.";
}
