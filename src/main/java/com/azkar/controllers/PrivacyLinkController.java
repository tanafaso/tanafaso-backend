package com.azkar.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class PrivacyLinkController {

  public static final String PRIVACY_POLICY_PATH = "/privacy-policy";

  @GetMapping(PRIVACY_POLICY_PATH)
  public RedirectView getAndroidStoreLink(
      RedirectAttributes attributes) {
    attributes.addFlashAttribute("flashAttribute", "redirectWithRedirectView");
    attributes.addAttribute("attribute", "redirectWithRedirectView");
    return new RedirectView(
        "https://www.freeprivacypolicy.com/live/9816e822-5877-4500-9bab-174ac83d56f4");
  }
}
