package com.azkar.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController extends BaseController {

  @GetMapping(value = "/", produces = JSON_CONTENT_TYPE)
  ResponseEntity home() {
    return ResponseEntity.ok().build();
  }
}
