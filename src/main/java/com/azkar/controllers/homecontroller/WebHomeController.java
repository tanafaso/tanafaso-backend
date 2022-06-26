package com.azkar.controllers.homecontroller;

import com.azkar.controllers.BaseController;
import com.azkar.repos.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebHomeController extends BaseController {

  public static final String HOME_PAGE_VIEW_NAME = "home";

  @GetMapping(value = "/")
  public String homePage(Model model) {
    return HOME_PAGE_VIEW_NAME;
  }
}
