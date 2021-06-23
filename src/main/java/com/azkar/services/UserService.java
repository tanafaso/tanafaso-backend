package com.azkar.services;

import com.azkar.entities.Friendship;
import com.azkar.entities.Friendship.Friend;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.entities.User.UserGroup;
import com.azkar.repos.FriendshipRepo;
import com.azkar.repos.GroupRepo;
import com.azkar.repos.UserRepo;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private static final String ENGLISH_CHARS_STRING_REGEX = "^[a-zA-Z]*$";
  @Autowired
  private UserRepo userRepo;
  @Autowired
  private FriendshipRepo friendshipRepo;
  @Autowired
  private GroupRepo groupRepo;

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
    addSabeqAsFriend(user);
    return user;
  }

  private void addSabeqAsFriend(User user) {
    Friendship friendship = Friendship.builder().userId(user.getId()).build();
    User sabeq = userRepo.findById(User.SABEQ_ID).get();
    Group binaryGroup = Group.builder()
        .usersIds(Arrays.asList(user.getId(), sabeq.getId()))
        .creatorId(sabeq.getId())
        .build();
    groupRepo.save(binaryGroup);
    Friend sabeqAsFriend = Friend.builder()
        .userId(sabeq.getId())
        .username(sabeq.getUsername())
        .firstName(sabeq.getFirstName())
        .lastName(sabeq.getLastName())
        .isPending(false)
        .groupId(binaryGroup.getId())
        .build();
    friendship.getFriends().add(sabeqAsFriend);
    friendshipRepo.save(friendship);
    UserGroup userGroup = UserGroup.builder()
        .totalScore(0)
        .monthScore(0)
        .invitingUserId(sabeq.getId())
        .groupId(binaryGroup.getId())
        .groupName("")
        .build();
    user.getUserGroups().add(userGroup);
    userRepo.save(user);
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
