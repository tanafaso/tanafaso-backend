package com.azkar.controllers;

import com.azkar.configs.authentication.UserPrincipal;
import com.azkar.entities.User;
import com.azkar.repos.UserRepo;
import org.springframework.security.core.context.SecurityContextHolder;

public class BaseController {

  protected UserPrincipal getCurrentUser() {
    return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  }

  protected User getCurrentUser(UserRepo userRepo) {
    return userRepo.findById(getCurrentUser().getUserId()).get();
  }
}
