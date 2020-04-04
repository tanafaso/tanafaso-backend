package com.azkar.payload;

import com.azkar.entities.User;

public class AddUserResponse implements Response {
  private User body;
  private Error error;

  @Override
  public Error getError() {
    return error;
  }

  @Override
  public void setError(Error error) {
    this.error = error;
  }

  @Override
  public Object getBody() {
    return body;
  }

  @Override
  public void setBody(Object body) {
    this.body = (User) body;
  }
}
