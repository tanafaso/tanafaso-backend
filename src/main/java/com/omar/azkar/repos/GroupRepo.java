package com.omar.azkar.repos;

import com.omar.azkar.entities.Group;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GroupRepo extends MongoRepository<Group, String> {

}
