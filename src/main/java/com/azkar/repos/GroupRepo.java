package com.azkar.repos;

import com.azkar.entities.Group;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GroupRepo extends MongoRepository<Group, String> {

}
