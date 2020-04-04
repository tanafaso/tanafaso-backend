package com.azkar.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

  @GetMapping(value = "/", produces = "application/json")
  Response home() {
    return new Response(true);
  }
}
