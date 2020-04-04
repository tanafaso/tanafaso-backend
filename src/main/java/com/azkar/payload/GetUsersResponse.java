package com.azkar.payload;

import com.azkar.entities.User;
import java.util.List;

public class GetUsersResponse implements Response {
  private List<User> body;
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
    this.body = (List<User>) body;
  }
}
