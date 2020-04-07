package com.azkar.requestbodies;

public interface RequestBody {

  boolean validate() throws RequestBodyException;
}
