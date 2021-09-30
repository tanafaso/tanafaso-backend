package com.azkar.services;

import com.azkar.entities.Friendship;
import com.azkar.entities.Friendship.Friend;
import com.azkar.entities.User;
import com.azkar.payload.utils.FeaturesVersions;
import com.azkar.payload.utils.VersionComparator;
import com.azkar.repos.FriendshipRepo;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FriendshipService {

  @Autowired
  FriendshipRepo friendshipRepo;

  public List<Friend> getFriendsLeaderboard(String apiVersion, User user) {
    Friendship friendship =
        friendshipRepo.findByUserId(user.getId());

    List<Friend> friends = friendship.getFriends();
    if (apiVersion == null
        || VersionComparator.compare(apiVersion, FeaturesVersions.SABEQ_ADDITION_VERSION) < 0) {
      friends = friends.stream().filter(friend -> !friend.getUserId().equals(User.SABEQ_ID))
          .collect(Collectors.toList());
    }
    return friends;
  }
}
