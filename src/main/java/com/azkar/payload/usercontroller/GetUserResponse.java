package com.azkar.payload.usercontroller;

import com.azkar.entities.User;
import com.azkar.payload.ResponseBase;

public class GetUserResponse extends ResponseBase<User> {

  public static final String USER_NOT_FOUND_ERROR = "User not found";
  public static final String SEARCH_PARAMETERS_NOT_SPECIFIED = "Search parameters not specified.";
}
