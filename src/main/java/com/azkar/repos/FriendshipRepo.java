package com.azkar.repos;

import com.azkar.entities.Friendship;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FriendshipRepo extends MongoRepository<Friendship, String> {

  List<Friendship> findByUserId1(String userId1);
}
