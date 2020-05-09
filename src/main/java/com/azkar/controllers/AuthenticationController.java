package com.azkar.controllers;

import static com.azkar.payload.authenticationcontroller.responses.EmailRegistrationResponse.PIN_ALREADY_SENT_TO_USER_ERROR;
import static com.azkar.payload.authenticationcontroller.responses.EmailRegistrationResponse.USER_ALREADY_REGISTERED_ERROR;

import com.azkar.entities.RegistrationEmailConfirmationState;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.authenticationcontroller.requests.EmailRegistrationRequestBody;
import com.azkar.payload.authenticationcontroller.responses.EmailRegistrationResponse;
import com.azkar.repos.RegistrationEmailConfirmationStateRepo;
import com.azkar.repos.UserRepo;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthenticationController extends BaseController {

  public static final String REGISTER_WITH_EMAIL_PATH = "/register/email";

  @Autowired
  private UserRepo userRepo;

  @Autowired
  private RegistrationEmailConfirmationStateRepo registrationPinRepo;

  @Autowired
  private JavaMailSender javaMailSender;

  @Autowired
  PasswordEncoder passwordEncoder;

  @GetMapping(value = REGISTER_WITH_EMAIL_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<EmailRegistrationResponse> registerWithEmail(
      @RequestBody EmailRegistrationRequestBody body) {
    EmailRegistrationResponse response = new EmailRegistrationResponse();
    body.validate();

    if (userRepo.existsByEmail(body.getEmail())) {
      response.setError(new Error(USER_ALREADY_REGISTERED_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    // TODO(omaryasser): Handle user registered before with facebook with the same email.

    if (registrationPinRepo.existsByEmail(body.getEmail())) {
      response.setError(new Error(PIN_ALREADY_SENT_TO_USER_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    int pin = generatePin();

    // TODO(issue#73): Beautify email confirmation body.
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom("azkar_email_name@azkaremaildomain.com");
    message.setSubject("A7la mesa 3aleeek, Azkar email confirmation");
    message.setText("The pin is: " + pin);
    message.setTo(body.getEmail());
    javaMailSender.send(message);

    registrationPinRepo.save(
        RegistrationEmailConfirmationState.builder()
            .email(body.getEmail())
            .password(passwordEncoder.encode(body.getPassword()))
            .pin(pin)
            .name(body.getName()).build());
    return ResponseEntity.ok(response);
  }

  private int generatePin() {
    int min = 1000_00;
    int max = 1000_000 - 1;
    return new Random(System.currentTimeMillis()).nextInt(max - min) + min;
  }
}
