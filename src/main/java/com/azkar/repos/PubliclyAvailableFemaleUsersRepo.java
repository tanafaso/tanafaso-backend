package com.azkar.repos;

import com.azkar.entities.PubliclyAvailableFemaleUser;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PubliclyAvailableFemaleUsersRepo extends
    MongoRepository<PubliclyAvailableFemaleUser, String> {

  Optional<PubliclyAvailableFemaleUser> findByUserId(String userId);
}
