package com.azkar.controllers;

import com.azkar.entities.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BaseController {

  @Autowired
  CurrentUser currentUser;
}
