package com.azkar.services;

import com.azkar.entities.Friendship;
import com.azkar.entities.User;
import com.azkar.repos.FriendshipRepo;
import com.azkar.repos.UserRepo;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

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


  public User buildNewUser(String email, String name) {
    return buildNewUser(email, name, /*encodedPassword=*/null);
  }

  public User buildNewUser(String email, String name, String encodedPassword) {
    return User.builder()
        .id(new ObjectId().toString())
        .email(email)
        .username(generateUsername(name))
        .name(name)
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
  private String generateUsername(String name) {
    name = name.replace(" ", "");
    name = name.toLowerCase();

    return name + '-' + userRepo.count();
  }
}
