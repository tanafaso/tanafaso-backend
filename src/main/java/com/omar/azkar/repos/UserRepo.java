package com.omar.azkar.repos;

import com.omar.azkar.entities.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends MongoRepository<User, String> {

  Optional<User> findByEmail(String email);
}
