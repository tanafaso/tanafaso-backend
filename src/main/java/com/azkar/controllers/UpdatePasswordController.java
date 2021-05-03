package com.azkar.controllers;

import com.azkar.entities.User;
import com.azkar.repos.UserRepo;
import java.time.Instant;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * This control is responsible for handling the reset password scenario. Since resetting password is
 * only allowed from a browser. This controller produces html pages directly and not JSON.
 */
@Controller()
public class UpdatePasswordController {

  public static final String UPDATE_PASSWORD_PATH = "/update_password";
  public static final String UPDATE_PASSWORD_VIEW_NAME = "update_password";
  public static final String INVALID_TOKEN_VIEW_NAME = "error_page";
  public static final String UPDATE_PASSWORD_SUCCESS_VIEW_NAME = "update_password_success";

  public static final String INVALID_TOKEN_ERROR =
      "هذا الرابط منتهي أو غير صحيح. من فضلك اطلب استعادة كلمة المرور مرة أخرى.";
  public static final String PASSWORD_MALFORMED_ERROR =
      "يجب ألا تقل كلمة المرور عن 8 أحرف.";

  @Autowired
  UserRepo userRepo;

  @Autowired
  PasswordEncoder passwordEncoder;

  @GetMapping("/update_password")
  public String verifyResetPasswordToken(
      @RequestParam String token, HttpServletResponse response, Model model) {
    Optional<User> user = userRepo.findByResetPasswordToken(token);
    if (isResetPasswordTokenValid(user)) {
      model.addAttribute("token", token);
      return UPDATE_PASSWORD_VIEW_NAME;
    } else {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      model.addAttribute("errorMessage", INVALID_TOKEN_ERROR);
      return INVALID_TOKEN_VIEW_NAME;
    }
  }

  private boolean isResetPasswordTokenValid(Optional<User> user) {
    return user.isPresent()
        && user.get().getResetPasswordTokenExpiryTime() > Instant.now().getEpochSecond();
  }

  @PostMapping(value = "update_password", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public String updatePassword(String token, String password, Model model,
      HttpServletResponse response) {
    if (password == null || password.length() < 8) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      model.addAttribute("errorMessage", PASSWORD_MALFORMED_ERROR);
      return INVALID_TOKEN_VIEW_NAME;
    }
    Optional<User> user = userRepo.findByResetPasswordToken(token);
    if (isResetPasswordTokenValid(user)) {
      user.get().setEncodedPassword(passwordEncoder.encode(password));
      userRepo.save(user.get());
      return UPDATE_PASSWORD_SUCCESS_VIEW_NAME;
    } else {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      model.addAttribute("errorMessage", INVALID_TOKEN_ERROR);
      return INVALID_TOKEN_VIEW_NAME;
    }
  }
}
