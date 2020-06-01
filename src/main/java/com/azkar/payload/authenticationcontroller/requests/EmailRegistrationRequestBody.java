package com.azkar.payload.authenticationcontroller.requests;

import com.azkar.payload.RequestBodyBase;
import com.azkar.payload.exceptions.BadRequestException;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class EmailRegistrationRequestBody extends RequestBodyBase {

  public static final String EMAIL_NOT_VALID_ERROR = "This email is not valid.";
  public static final String NAME_EMPTY_ERROR = "The name can not be empty.";

  private static final int MINIMUM_PASSWORD_CHARACTERS = 8;
  public static final String PASSWORD_CHARACTERS_LESS_THAN_MIN_ERROR =
      String.format("The password must be of at least %d characters.", MINIMUM_PASSWORD_CHARACTERS);

  private String email;
  private String password;
  private String name;

  // For more information regarding the regex used for email validation, please refer to:
  // https://www.tutorialspoint.com/validate-email-address-in-java
  private static void validateEmail(String email) throws BadRequestException {
    String regex = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";
    if (!email.matches(regex)) {
      throw new BadRequestException(EMAIL_NOT_VALID_ERROR);
    }
  }

  private static void validatePassword(String password) throws BadRequestException {
    if (password.length() < MINIMUM_PASSWORD_CHARACTERS) {
      throw new BadRequestException(PASSWORD_CHARACTERS_LESS_THAN_MIN_ERROR);
    }
  }

  private static void validateName(String name) throws BadRequestException {
    if (name.isEmpty()) {
      throw new BadRequestException(NAME_EMPTY_ERROR);
    }
  }

  @Override public void validate() throws BadRequestException {
    checkNotNull(email, password, name);
    validateEmail(email);
    validatePassword(password);
    validateName(name);
  }

}
