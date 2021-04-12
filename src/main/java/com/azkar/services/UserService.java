package com.azkar.services;

import com.azkar.entities.Friendship;
import com.azkar.entities.User;
import com.azkar.repos.FriendshipRepo;
import com.azkar.repos.UserRepo;
import java.util.Optional;
import java.util.Random;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private static final String ENGLISH_CHARS_STRING_REGEX = "^[a-zA-Z]*$";
  @Autowired
  private UserRepo userRepo;
  @Autowired
  private FriendshipRepo friendshipRepo;

  public User loadUserById(String id) {
    Optional<User> user = userRepo.findById(id);
    if (user.isPresent()) {
      return user.get();
    }
    return null;
  }


  public User buildNewUser(String email, String firstName, String lastName) {
    return buildNewUser(email, firstName, lastName, /*encodedPassword=*/null);
  }

  public User buildNewUser(String email, String firstName, String lastName,
      String encodedPassword) {
    return User.builder()
        .id(new ObjectId().toString())
        .email(email)
        .username(generateUsername(firstName, lastName))
        .firstName(firstName)
        .lastName(lastName)
        .encodedPassword(encodedPassword)
        .build();
  }

  /*
    Adds a new user to the database as well as adding all of the dependencies that should be created
    with a new user.
  */
  public User addNewUser(User user) {
    userRepo.save(user);
    Friendship friendship = Friendship.builder().userId(user.getId()).build();
    friendshipRepo.insert(friendship);
    return user;
  }

  // Note: Only this generator should be able to create usernames with the special character '-',
  // but users shouldn't be able to use this character while changing their usernames.
  private String generateUsername(String firstName, String lastName) {
    firstName = firstName.replace(" ", "");
    lastName = lastName.replace(" ", "");
    firstName = firstName.toLowerCase();
    lastName = lastName.toLowerCase();

    while (true) {
      boolean nameCanBePrefix = firstName.matches(ENGLISH_CHARS_STRING_REGEX) && lastName
          .matches(ENGLISH_CHARS_STRING_REGEX);
      String usernamePrefix = "";
      String randomUsernameSuffix;
      if (nameCanBePrefix) {
        usernamePrefix = firstName + "-" + lastName + "-";
        randomUsernameSuffix = generateRandomString(4);
      } else {
        randomUsernameSuffix = generateRandomString(8);
      }
      if (!userRepo.findByUsername(usernamePrefix + randomUsernameSuffix).isPresent()) {
        return usernamePrefix + randomUsernameSuffix;
      }
    }
  }

  private String generateRandomString(int length) {
    int minLimit = ('a');
    int maxLimit = ('z');
    Random random = new Random();

    return random.ints(minLimit, maxLimit + 1)
        .limit(length)
        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
        .toString();
  }
}
