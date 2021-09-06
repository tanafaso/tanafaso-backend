package com.azkar.controllers;

import com.azkar.repos.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class HomeController extends BaseController {

  @Autowired
  public UserRepo userRepo;

  @GetMapping(value = "/")
  public RedirectView getHome(RedirectAttributes attributes) {
    attributes.addFlashAttribute("flashAttribute", "redirectWithRedirectView");
    attributes.addAttribute("attribute", "redirectWithRedirectView");
    return new RedirectView("https://play.google.com/store/apps/details?id=com.tanafaso.azkar");
  }
}
