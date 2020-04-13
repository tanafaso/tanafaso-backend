package com.azkar.payload.homecontroller;

import com.azkar.entities.User;
import com.azkar.payload.ResponseBase;

public class GetHomeResponse extends ResponseBase<User> {

  public static final String ERROR_USER_NOT_FOUND = "Cannot find logged in user.";

}
