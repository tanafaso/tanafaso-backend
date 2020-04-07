package com.azkar.repos;

import com.azkar.entities.Friendship;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FriendshipRepo extends MongoRepository<Friendship, String> {

  List<Friendship> findByRequesterId(String requesterId);

  List<Friendship> findByResponderId(String responderId);

  Optional<Friendship> findByRequesterIdAndResponderId(String requesterId, String responderId);

}
