package com.azkar.controllers;

import static com.azkar.payload.authenticationcontroller.responses.EmailRegistrationResponse.PIN_ALREADY_SENT_TO_USER_ERROR;
import static com.azkar.payload.authenticationcontroller.responses.EmailRegistrationResponse.USER_ALREADY_REGISTERED_ERROR;
import static com.azkar.payload.authenticationcontroller.responses.EmailRegistrationResponse.USER_ALREADY_REGISTERED_WITH_FACEBOOK;

import com.azkar.entities.RegistrationEmailConfirmationState;
import com.azkar.entities.User;
import com.azkar.entities.User.UserFacebookData;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.authenticationcontroller.requests.EmailRegistrationRequestBody;
import com.azkar.payload.authenticationcontroller.requests.EmailVerificationRequestBody;
import com.azkar.payload.authenticationcontroller.requests.FacebookAuthenticationRequest;
import com.azkar.payload.authenticationcontroller.responses.EmailRegistrationResponse;
import com.azkar.payload.authenticationcontroller.responses.EmailVerificationResponse;
import com.azkar.payload.authenticationcontroller.responses.FacebookAuthenticationResponse;
import com.azkar.repos.RegistrationEmailConfirmationStateRepo;
import com.azkar.repos.UserRepo;
import com.azkar.services.JwtService;
import com.azkar.services.UserService;
import java.util.Optional;
import java.util.Random;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthenticationController extends BaseController {

  public static final String LOGIN_WITH_FACEBOOK_PATH = "/login/facebook";
  public static final String REGISTER_WITH_EMAIL_PATH = "/register/email";
  public static final String VERIFY_EMAIL_PATH = "/verify/email";

  private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

  @Autowired
  UserRepo userRepo;

  @Autowired
  UserService userService;

  @Autowired
  JwtService jwtService;
  @Autowired
  PasswordEncoder passwordEncoder;
  @Autowired
  private RegistrationEmailConfirmationStateRepo registrationPinRepo;
  @Autowired
  private JavaMailSender javaMailSender;
  private RestTemplate restTemplate;

  public AuthenticationController(RestTemplateBuilder restTemplateBuilder) {
    restTemplate = restTemplateBuilder.build();
  }

  @GetMapping(value = REGISTER_WITH_EMAIL_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<EmailRegistrationResponse> registerWithEmail(
      @RequestBody EmailRegistrationRequestBody body) {
    EmailRegistrationResponse response = new EmailRegistrationResponse();
    body.validate();

    if (userRepo.existsByEmail(body.getEmail())) {
      response.setError(new Error(USER_ALREADY_REGISTERED_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    if (userRepo.existsByUserFacebookData_Email(body.getEmail())) {
      response.setError(new Error(USER_ALREADY_REGISTERED_WITH_FACEBOOK));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    if (registrationPinRepo.existsByEmail(body.getEmail())) {
      response.setError(new Error(PIN_ALREADY_SENT_TO_USER_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    int pin = generatePin();

    sendVerificationEmail(body.getEmail(), pin);

    registrationPinRepo.save(
        RegistrationEmailConfirmationState.builder()
            .email(body.getEmail())
            .password(passwordEncoder.encode(body.getPassword()))
            .pin(pin)
            .name(body.getName()).build());
    return ResponseEntity.ok(response);
  }

  @GetMapping(value = VERIFY_EMAIL_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<EmailVerificationResponse> verifyEmail(
      @RequestBody EmailVerificationRequestBody body) {
    EmailVerificationResponse response = new EmailVerificationResponse();
    body.validate();

    if (userRepo.existsByEmail(body.getEmail())) {
      response.setError(new Error(EmailVerificationResponse.EMAIL_ALREADY_VERIFIED_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    Optional<RegistrationEmailConfirmationState> registrationEmailConfirmationState =
        registrationPinRepo.findByEmail(body.getEmail());
    if (!registrationEmailConfirmationState.isPresent()
        || registrationEmailConfirmationState.get().getPin() != body.getPin().intValue()) {
      response.setError(new Error(EmailVerificationResponse.VERIFICATION_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    userService.addNewUser(
        User.builder()
            .name(registrationEmailConfirmationState.get().getName())
            .email(registrationEmailConfirmationState.get().getEmail())
            .encodedPassword(registrationEmailConfirmationState.get().getPassword())
            .build()
    );
    registrationPinRepo.delete(registrationEmailConfirmationState.get());

    return ResponseEntity.ok(response);
  }

  /**
   * <p>
   * This mapping is used in two cases: 1- A new user is authenticating with facebook. 2- An
   * existing user is authenticating with facebook because their JWT token is expired or or they
   * don't have it in their session.
   * </p>
   * <p>
   * This request is expected to called by a non-logged in user so the security context
   * authentication is expected to be not set.
   * </p>
   */
  @PutMapping(value = LOGIN_WITH_FACEBOOK_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<FacebookAuthenticationResponse> loginWithFacebook(
      @RequestBody FacebookAuthenticationRequest requestBody) {
    requestBody.validate();
    FacebookAuthenticationResponse response = new FacebookAuthenticationResponse();

    if (!(SecurityContextHolder.getContext()
        .getAuthentication() instanceof AnonymousAuthenticationToken)) {
      response
          .setError(new Error(FacebookAuthenticationResponse.USER_ALREADY_LOGGED_IN));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    FacebookBasicProfileResponse facebookResponse = assertUserFacebookData(requestBody);

    if (facebookResponse == null) {
      response
          .setError(new Error(FacebookAuthenticationResponse.AUTHENTICATION_WITH_FACEBOOK_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    String jwtToken;
    try {
      User user =
          userRepo.findByUserFacebookData_UserId(facebookResponse.getId()) // Case 2
              .orElse(userService.buildNewUser(facebookResponse.email, // Case 1
                  facebookResponse.name));
      UserFacebookData userFacebookData = UserFacebookData.builder()
          .accessToken(requestBody.getToken())
          .userId(facebookResponse.id)
          .name(facebookResponse.name)
          .email(facebookResponse.email).build();
      user.setUserFacebookData(userFacebookData);
      userRepo.save(user);
      jwtToken = jwtService.generateToken(user);
    } catch (Exception e) {
      response
          .setError(new Error(FacebookAuthenticationResponse.AUTHENTICATION_WITH_FACEBOOK_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setBearerAuth(jwtToken);
    ResponseEntity<FacebookAuthenticationResponse> responseEntity =
        new ResponseEntity<>(response, httpHeaders, HttpStatus.OK);
    return responseEntity;
  }


  /**
   * This mapping should only be used in case of a logged in user who wants to connect their account
   * with facebook. Note, that maybe they have already connected an account; in that case the new
   * facebook information will override the old one. This request is expected to called by a logged
   * in user so the security context authentication is expected to be set.
   */
  @PutMapping(value = "/connect/facebook", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<FacebookAuthenticationResponse> connectFacebook(
      @RequestBody FacebookAuthenticationRequest requestBody) {
    requestBody.validate();
    FacebookAuthenticationResponse response = new FacebookAuthenticationResponse();

    FacebookBasicProfileResponse facebookResponse = assertUserFacebookData(requestBody);

    if (facebookResponse == null) {
      response
          .setError(new Error(FacebookAuthenticationResponse.AUTHENTICATION_WITH_FACEBOOK_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    User user =
        userRepo.findByUserFacebookData_UserId(facebookResponse.getId())
            .orElse(userRepo.findById(getCurrentUser().getUserId()).get());
    if (!user.getId().equals(getCurrentUser().getUserId())) {
      logger.warn("The user is attempting to connect a facebook account already connected to "
          + "another account.");
      response
          .setError(new Error(FacebookAuthenticationResponse.SOMEONE_ELSE_ALREADY_CONNECTED_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    UserFacebookData userFacebookData = UserFacebookData.builder()
        .accessToken(requestBody.getToken())
        .userId(facebookResponse.id)
        .name(facebookResponse.name)
        .email(facebookResponse.email).build();
    user.setUserFacebookData(userFacebookData);
    userRepo.save(user);

    return ResponseEntity.ok(response);
  }

  private void sendVerificationEmail(String email, int pin) {
    // TODO(issue#73): Beautify email confirmation body.
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom("azkar_email_name@azkaremaildomain.com");
    message.setSubject("A7la mesa 3aleeek, Azkar email confirmation");
    message.setText("The pin is: " + pin);
    message.setTo(email);
    javaMailSender.send(message);
  }

  private int generatePin() {
    final int min = 100_000;
    final int max = 1000_000 - 1;
    return new Random(System.currentTimeMillis()).nextInt(max - min) + min;
  }

  private FacebookBasicProfileResponse assertUserFacebookData(FacebookAuthenticationRequest body) {
    String facebookGraphApiUril =
        "https://graph.facebook.com/v7.0/me?fields=id,name,email&access_token=" + body.getToken();
    FacebookBasicProfileResponse facebookResponse = restTemplate.getForObject(
        facebookGraphApiUril,
        FacebookBasicProfileResponse.class);

    if (facebookResponse.id == null || !facebookResponse.id.equals(body.getFacebookUserId())) {
      return null;
    }

    return facebookResponse;

  }

  @Data
  public static class FacebookBasicProfileResponse {

    String id;
    String name;
    String email;
  }
}