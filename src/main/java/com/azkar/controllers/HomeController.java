package com.azkar.controllers;


import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class HomeController extends BaseController {


  @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity home() {
    return ResponseEntity.ok().build();
  }
}
