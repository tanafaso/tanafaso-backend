package com.omar.azkar.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

  static class Response {
    boolean success;

    public Response(boolean success) {
      this.success = success;
    }

    public boolean isSuccess() {
      return success;
    }
  }

  @GetMapping(value = "/", produces = "application/json")
  Response home() {
    return new Response(true);
  }
}
