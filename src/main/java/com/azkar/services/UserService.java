package com.azkar.services;

import com.azkar.entities.Friendship;
import com.azkar.entities.User;
import com.azkar.repos.FriendshipRepo;
import com.azkar.repos.UserRepo;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private static final int MAX_EXPECTED_NAME_MATCHES = 100;
  private static final int MAX_USERNAME_GENERATION_TRIALS = 200;
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

  public User buildNewUser(String email, String name) throws UsernameGenerationException {
    return User.builder()
        .id(new ObjectId().toString())
        .email(email)
        .username(generateUsername(name.replace(" ", "")))
        .name(name)
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

  private String generateUsername(String name) throws UsernameGenerationException {
    for (int i = 0; i < MAX_USERNAME_GENERATION_TRIALS; i++) {
      int randomSuffix = ThreadLocalRandom.current().nextInt(1, MAX_EXPECTED_NAME_MATCHES);
      String randomUsername = name + randomSuffix;
      if (!userRepo.findByUsername(randomUsername).isPresent()) {
        return randomUsername;
      }
    }
    throw new UsernameGenerationException();
  }
}
