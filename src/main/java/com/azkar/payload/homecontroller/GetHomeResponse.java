package com.azkar.payload.homecontroller;

import com.azkar.entities.User;
import com.azkar.payload.ResponseBase;

public class GetHomeResponse extends ResponseBase<User> {

  public static final String ERROR_USER_NOT_FOUND = "Can not find logged in user.";

}
