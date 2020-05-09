package com.azkar.repos;

import com.azkar.entities.RegistrationEmailConfirmationState;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RegistrationEmailConfirmationStateRepo extends
    MongoRepository<RegistrationEmailConfirmationState, String> {

  boolean existsByEmail(String email);
}
