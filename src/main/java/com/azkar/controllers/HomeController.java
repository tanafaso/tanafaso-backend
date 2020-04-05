package com.azkar.controllers;

import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

  @GetMapping(value = "/", produces = "application/json")
  ResponseEntity home() {
    return ResponseEntity.ok().build();
  }
}
