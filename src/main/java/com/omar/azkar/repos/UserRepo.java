package com.omar.azkar.repos;

import com.omar.azkar.entities.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepo extends MongoRepository<User, Integer> {
    Optional<User> findById(String id);

}
