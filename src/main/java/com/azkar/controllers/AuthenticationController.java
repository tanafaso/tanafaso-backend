package com.azkar.controllers;

import com.azkar.entities.RegistrationEmailConfirmationState;
import com.azkar.entities.User;
import com.azkar.entities.User.UserFacebookData;
import com.azkar.payload.ResponseBase.Status;
import com.azkar.payload.authenticationcontroller.requests.EmailLoginRequestBody;
import com.azkar.payload.authenticationcontroller.requests.EmailRegistrationRequestBody;
import com.azkar.payload.authenticationcontroller.requests.EmailVerificationRequestBody;
import com.azkar.payload.authenticationcontroller.requests.FacebookAuthenticationRequest;
import com.azkar.payload.authenticationcontroller.requests.ResetPasswordRequest;
import com.azkar.payload.authenticationcontroller.responses.EmailLoginResponse;
import com.azkar.payload.authenticationcontroller.responses.EmailRegistrationResponse;
import com.azkar.payload.authenticationcontroller.responses.EmailVerificationResponse;
import com.azkar.payload.authenticationcontroller.responses.FacebookAuthenticationResponse;
import com.azkar.payload.authenticationcontroller.responses.ResetPasswordResponse;
import com.azkar.repos.RegistrationEmailConfirmationStateRepo;
import com.azkar.repos.UserRepo;
import com.azkar.services.JwtService;
import com.azkar.services.UserService;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import lombok.Data;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
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
  public static final String LOGIN_WITH_EMAIL_PATH = "/login/email";
  public static final String RESET_PASSWORD_PATH = "/reset_password";
  private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
  private static final long RESET_PASSWORD_EXPIRY_TIME_SECONDS = 30 * 60;

  private static final String RESET_PASSWORD_EMAIL_TEMPLATE_PATH =
      "emailTemplates/reset_password_email.html";
  private static final String VERIFY_EMAIL_TEMPLATE_PATH = "emailTemplates/verify_email.html";

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

  @PutMapping(value = REGISTER_WITH_EMAIL_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<EmailRegistrationResponse> registerWithEmail(
      @RequestBody EmailRegistrationRequestBody body)
      throws MessagingException, IOException {
    EmailRegistrationResponse response = new EmailRegistrationResponse();
    body.validate();

    if (userRepo.existsByEmail(body.getEmail())) {
      response.setStatus(new Status(Status.USER_ALREADY_REGISTERED_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    if (userRepo.existsByUserFacebookData_Email(body.getEmail())) {
      response.setStatus(new Status(Status.USER_ALREADY_REGISTERED_WITH_FACEBOOK));
      return ResponseEntity.badRequest().body(response);
    }

    if (registrationPinRepo.existsByEmail(body.getEmail())) {
      logger.warn("A user with email %s is trying to register more than once.", body.getEmail());
    }
    RegistrationEmailConfirmationState registrationEmailConfirmationState =
        registrationPinRepo.findByEmail(body.getEmail()).orElse(
            RegistrationEmailConfirmationState.builder()
                .email(body.getEmail())
                .password(passwordEncoder.encode(body.getPassword()))
                .firstName(body.getFirstName())
                .lastName(body.getLastName())
                .build());

    int pin = generatePin();
    registrationEmailConfirmationState.setPin(pin);
    sendVerificationEmail(body.getEmail(), pin);

    registrationPinRepo.save(registrationEmailConfirmationState);

    return ResponseEntity.ok(response);
  }

  @PutMapping(value = VERIFY_EMAIL_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<EmailVerificationResponse> verifyEmail(
      @RequestBody EmailVerificationRequestBody body) {
    EmailVerificationResponse response = new EmailVerificationResponse();
    body.validate();

    if (userRepo.existsByEmail(body.getEmail())) {
      response.setStatus(new Status(Status.EMAIL_ALREADY_VERIFIED_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    Optional<RegistrationEmailConfirmationState> registrationEmailConfirmationState =
        registrationPinRepo.findByEmail(body.getEmail());
    if (!registrationEmailConfirmationState.isPresent()
        || registrationEmailConfirmationState.get().getPin() != body.getPin().intValue()) {
      response.setStatus(new Status(Status.VERIFICATION_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    User user = userService.buildNewUser(registrationEmailConfirmationState.get().getEmail(),
        registrationEmailConfirmationState.get().getFirstName(),
        registrationEmailConfirmationState.get().getLastName(),
        registrationEmailConfirmationState.get().getPassword());

    userService.addNewUser(user);
    registrationPinRepo.delete(registrationEmailConfirmationState.get());

    return ResponseEntity.ok(response);
  }

  @PutMapping(value = LOGIN_WITH_EMAIL_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<EmailLoginResponse> loginWithEmail(
      @RequestBody EmailLoginRequestBody body) {
    EmailLoginResponse response = new EmailLoginResponse();
    body.validate();

    if (!(SecurityContextHolder.getContext()
        .getAuthentication() instanceof AnonymousAuthenticationToken)) {
      response
          .setStatus(new Status(Status.USER_ALREADY_LOGGED_IN_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    if (registrationPinRepo.existsByEmail(body.getEmail())) {
      response.setStatus(new Status(Status.EMAIL_NOT_VERIFIED_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    Optional<User> user = userRepo.findByEmail(body.getEmail());
    if (!user.isPresent() || !passwordEncoder.matches(
        body.getPassword(),
        user.get().getEncodedPassword())) {
      response.setStatus(new Status(Status.EMAIL_PASSWORD_COMBINATION_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    String jwtToken = null;
    try {
      jwtToken = jwtService.generateToken(user.get());
    } catch (UnsupportedEncodingException e) {
      logger.error("Error when generating a verified user token: " + e.getStackTrace());
      response
          .setStatus(new Status(Status.LOGIN_WITH_EMAIL_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setBearerAuth(jwtToken);
    ResponseEntity<EmailLoginResponse> responseEntity =
        new ResponseEntity(response, httpHeaders, HttpStatus.OK);
    return responseEntity;
  }

  @PostMapping(value = RESET_PASSWORD_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResetPasswordResponse> resetPassword(
      @RequestBody ResetPasswordRequest request) throws MessagingException, IOException {
    request.validate();
    Optional<User> user = userRepo.findByEmail(request.getEmail());
    if (user.isPresent()) {
      String resetPasswordToken = UUID.randomUUID().toString();
      user.get().setResetPasswordToken(resetPasswordToken);
      user.get().setResetPasswordTokenExpiryTime(
          Instant.now().getEpochSecond() + RESET_PASSWORD_EXPIRY_TIME_SECONDS);
      userRepo.save(user.get());
      sendResetPasswordEmail(request.getEmail(), resetPasswordToken);
      return ResponseEntity.ok(new ResetPasswordResponse());
    } else {
      ResetPasswordResponse errorResponse = new ResetPasswordResponse();
      errorResponse.setStatus(new Status(Status.USER_NOT_FOUND_ERROR));
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
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
          .setStatus(new Status(Status.USER_ALREADY_LOGGED_IN_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    FacebookBasicProfileResponse facebookResponse = assertUserFacebookData(requestBody);

    if (facebookResponse == null) {
      response
          .setStatus(new Status(Status.AUTHENTICATION_WITH_FACEBOOK_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    String jwtToken;
    try {
      // Case 2
      User user =
          userRepo.findByUserFacebookData_UserId(facebookResponse.getId()).orElse(null);
      if (user == null) {
        // Case 1
        user = userService.buildNewUser(facebookResponse.email, facebookResponse.firstName,
            facebookResponse.lastName);
        user = userService.addNewUser(user);
      }

      UserFacebookData userFacebookData = UserFacebookData.builder()
          .accessToken(requestBody.getToken())
          .userId(facebookResponse.id)
          .firstName(facebookResponse.firstName)
          .lastName(facebookResponse.lastName)
          .email(facebookResponse.email).build();
      user.setUserFacebookData(userFacebookData);
      user.setFirstName(userFacebookData.getFirstName());
      user.setLastName(userFacebookData.getLastName());
      userRepo.save(user);
      jwtToken = jwtService.generateToken(user);
    } catch (Exception e) {
      response
          .setStatus(new Status(Status.AUTHENTICATION_WITH_FACEBOOK_ERROR));
      return ResponseEntity.badRequest().body(response);
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
          .setStatus(new Status(Status.AUTHENTICATION_WITH_FACEBOOK_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    User user =
        userRepo.findByUserFacebookData_UserId(facebookResponse.getId())
            .orElse(userRepo.findById(getCurrentUser().getUserId()).get());
    if (!user.getId().equals(getCurrentUser().getUserId())) {
      logger.warn("The user is attempting to connect a facebook account already connected to "
          + "another account.");
      response
          .setStatus(new Status(Status.SOMEONE_ELSE_ALREADY_CONNECTED_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    UserFacebookData userFacebookData = UserFacebookData.builder()
        .accessToken(requestBody.getToken())
        .userId(facebookResponse.id)
        .firstName(facebookResponse.firstName)
        .lastName(facebookResponse.lastName)
        .email(facebookResponse.email).build();
    user.setUserFacebookData(userFacebookData);
    userRepo.save(user);

    return ResponseEntity.ok(response);
  }

  private void sendVerificationEmail(String email, int pin)
      throws MessagingException, IOException {
    String subject = "تأكيد البريد الإلكتروني";
    String text = getEmailTemplate(VERIFY_EMAIL_TEMPLATE_PATH)
        .replaceAll("PIN", "" + pin);
    sendEmail(email, subject, text);
  }

  private void sendResetPasswordEmail(String email, String token)
      throws MessagingException, IOException {
    String subject = "إعادة ضبط كلمة المرور";
    String url = String.format("https://www.tanafaso.com/update_password?token=%s", token);
    // adding a random number at the end to make sure that gmail does not collapse the email ending.
    String text = getEmailTemplate(RESET_PASSWORD_EMAIL_TEMPLATE_PATH)
        .replaceAll("URL", url)
        .replaceAll("RANDOM", RandomStringUtils.randomNumeric(2));
    sendEmail(email, subject, text);
  }

  private String getEmailTemplate(String path) throws IOException {
    return IOUtils.toString(
        new InputStreamReader(new ClassPathResource(path).getInputStream()));
  }

  private void sendEmail(String email, String subject, String body)
      throws MessagingException, UnsupportedEncodingException {
    MimeMessage mimeMessage = javaMailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
    InternetAddress fromAddress = new InternetAddress("azkar_email_name@azkaremaildomain.com",
        "تنافسوا");
    helper.setFrom(fromAddress);
    helper.setSubject(subject);
    helper.setText(body, /* html= */ true);
    helper.setTo(email);
    javaMailSender.send(mimeMessage);
  }

  private int generatePin() {
    final int min = 100_000;
    final int max = 1000_000 - 1;
    return new Random(System.currentTimeMillis()).nextInt(max - min) + min;
  }

  private FacebookBasicProfileResponse assertUserFacebookData(FacebookAuthenticationRequest body) {
    String facebookGraphApiUril =
        "https://graph.facebook.com/v7.0/me?fields=id,first_name,last_name,email&access_token="
            + body.getToken();
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
    @JsonProperty("first_name")
    String firstName;
    @JsonProperty("last_name")
    String lastName;
    String email;
  }
}
