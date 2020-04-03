package com.omar.azkar.services;

import com.omar.azkar.entities.User;
import com.omar.azkar.repos.UserRepo;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  @Autowired
  private UserRepo userRepository;

  public User loadUserById(String id) {
    Optional<User> user = userRepository.findById(id);
    if (user.isPresent()) {
      return user.get();
    }
    return null;
  }
}
