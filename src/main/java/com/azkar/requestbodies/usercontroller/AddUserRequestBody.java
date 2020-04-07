package com.azkar.requestbodies.usercontroller;

import com.azkar.requestbodies.RequestBody;
import com.azkar.requestbodies.RequestBodyException;

public class AddUserRequestBody implements RequestBody {

  private static final String kRequiredFieldFoundEmptyError =
      "One of the fields required is found empty.";
  private String name;
  private String email;
  private String username;

  @Override public boolean validate() throws RequestBodyException {
    if (email == null ||
        email.isEmpty() ||
        username == null ||
        username.isEmpty()) {
      throw new RequestBodyException(kRequiredFieldFoundEmptyError);
    }
    return true;
  }
}
