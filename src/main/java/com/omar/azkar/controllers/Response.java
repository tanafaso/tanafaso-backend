package com.omar.azkar.controllers;

public class Response {

  boolean success;

  public Response(boolean success) {
    this.success = success;
  }

  public boolean isSuccess() {
    return success;
  }
}
