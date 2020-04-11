package com.azkar.controllers;

import com.azkar.configs.authentication.UserPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;

public class BaseController {

  protected UserPrincipal getCurrentUser() {
    return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  }
}
