package com.azkar.payload;

public interface Response {
  public Error getError();

  public void setError(Error error);

  public Object getBody();

  public void setBody(Object body);
}
