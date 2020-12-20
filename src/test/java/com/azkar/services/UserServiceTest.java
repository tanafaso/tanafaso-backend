package com.azkar.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.azkar.TestBase;
import com.azkar.entities.User;
import com.azkar.repos.UserRepo;
import com.google.common.collect.Iterators;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class UserServiceTest extends TestBase {

  @Autowired
  UserService userService;

  @Autowired
  UserRepo userRepo;

  @Test
  public void buildAndAddNewUser_normalScenario_shouldSucceed() {
    String email = "example_email@example_domain.com";
    String name = "example_name";

    userService.addNewUser(userService.buildNewUser(email, name));

    User user = Iterators.getOnlyElement(userRepo.findAll().iterator());
    assertThat(user.getEmail(), equalTo(email));
    assertThat(user.getName(), equalTo(name));
    assertThatUsernameIsValid(user.getUsername());
  }

  @Test
  public void buildAndAddNewUser_nameHasUpperCaseChars_usernameShouldNotHaveUpperCaseChars() {
    String email = "example_email@example_domain.com";
    String name = "eXample_name";

    userService.addNewUser(userService.buildNewUser(email, name));

    User user = Iterators.getOnlyElement(userRepo.findAll().iterator());
    assertThat(user.getEmail(), equalTo(email));
    assertThat(user.getName(), equalTo(name));
    assertThatUsernameIsValid(user.getUsername());
  }

  @Test
  public void buildAndAddNewUser_nameHasSpaces_usernameShouldNotHaveSpaces() {
    String email = "example_email@example_domain.com";
    String name = "Example Name";

    userService.addNewUser(userService.buildNewUser(email, name));

    User user = Iterators.getOnlyElement(userRepo.findAll().iterator());
    assertThat(user.getEmail(), equalTo(email));
    assertThat(user.getName(), equalTo(name));
    assertThatUsernameIsValid(user.getUsername());
  }

  @Test
  public void buildAndAddNewUser_HundredUsersWithSameName_usernamesShouldBeUnique() {
    String email = "example_email@example_domain.com";
    String name = "Example Name";

    for (int i = 0; i < 100; i++) {
      userService.addNewUser(userService.buildNewUser(email + "i", name));
    }

    assertThat(userRepo.count(), is(100L));
    List<User> users = userRepo.findAll();
    HashSet<String> usernames = new HashSet<>();
    for (User user : users) {
      assertThat("Usernames are unique", !usernames.contains(user.getUsername()));
      assertThatUsernameIsValid(user.getUsername());

      usernames.add(user.getUsername());
    }
  }

  private void assertThatUsernameIsValid(String username) {
    assertThat(String.format("Username: %s has only lower case characters", username),
        username.equals(username.toLowerCase()));
    assertThat(String.format("Username: %s doesn't contain spaces", username),
        username.indexOf(' ') == -1);
    assertThat(
        String.format("Username: %s has only english alphabet, numbers, underscores and "
                + "possibly a hyphen",
            username),
        username.matches("[a-zA-Z0-9_-]*"));
  }
}
