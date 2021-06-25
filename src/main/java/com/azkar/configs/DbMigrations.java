package com.azkar.configs;

import com.azkar.entities.Challenge;
import com.azkar.entities.Friendship;
import com.azkar.entities.Friendship.Friend;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.entities.User.UserGroup;
import com.github.mongobee.Mongobee;
import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Service;

@ChangeLog
@Configuration
@EnableMongoRepositories(basePackages = {"com.azkar.repos"})
@Service
public class DbMigrations {

  private static final Logger logger = LoggerFactory.getLogger(DbMigrations.class);

  @Autowired
  MongoTemplate mongoTemplate;
  @Value("${spring.data.mongodb.uri}")
  private String dbUri;
  @Value("${DATABASE_NAME}")
  private String dbName;

  @Bean
  @Lazy(value = false)
  public Mongobee mongobee() {
    Mongobee runner = new Mongobee(dbUri);
    runner.setDbName(dbName);         // host must be set if not set in URI
    runner.setChangeLogsScanPackage(
        "com.azkar.configs"); // the package to be scanned for changesets
    runner.setMongoTemplate(mongoTemplate);
    return runner;
  }

  // This change set has failed because a lot of corner cases weren't handled which led to leaving
  // the database in an inconsistent state. Th next change set aims to remove everything about
  // sabeq.
  /*
  @ChangeSet(order = "0001", id = "addSabeq", author = "")
  public void failedAddSabeq(MongoTemplate mongoTemplate) {
    // Check the unicode displayed at https://emojipedia.org/racing-car/
    User sabeq = User.builder()
        .id(User.SABEQ_ID)
        .firstName("سابق")
        .lastName("\uD83C\uDFCE️️")
        .username("sabeq")
        .build();
    mongoTemplate.save(sabeq);

    mongoTemplate.findAll(Friendship.class).stream().forEach(friendship -> {
      // Add sabeq as a friend to this user.
      Group binaryGroup = Group.builder()
          .usersIds(Arrays.asList(friendship.getUserId(), sabeq.getId()))
          .creatorId(sabeq.getId())
          .build();
      mongoTemplate.save(binaryGroup);
      Friend sabeqAsFriend = Friend.builder()
          .userId(sabeq.getId())
          .username(sabeq.getUsername())
          .firstName(sabeq.getFirstName())
          .lastName(sabeq.getLastName())
          .isPending(false)
          .groupId(binaryGroup.getId())
          .build();
      friendship.getFriends().add(0, sabeqAsFriend);

      // Copy the user's personal challenges to a challenge between the user and sabeq.
      User otherUser = mongoTemplate.findById(friendship.getUserId(), User.class);
      UserGroup userGroup = UserGroup.builder()
          .totalScore(0)
          .monthScore(0)
          .invitingUserId(sabeq.getId())
          .groupId(binaryGroup.getId())
          .groupName("")
          .build();
      otherUser.getPersonalChallenges().stream().forEach(personalChallenge -> {
        Challenge originalPersonalChallenge =
            mongoTemplate.findById(personalChallenge.getId(), Challenge.class);
        if (originalPersonalChallenge == null) {
          return;
        }
        originalPersonalChallenge.setGroupId(binaryGroup.getId());
        mongoTemplate.save(originalPersonalChallenge);

        // Migrate the user progress.
        originalPersonalChallenge.setSubChallenges(personalChallenge.getSubChallenges());
        if (originalPersonalChallenge.finished()) {
          userGroup.setTotalScore(userGroup.getTotalScore() + 1);
        }
        otherUser.getUserChallenges().add(originalPersonalChallenge);
      });

      otherUser.getUserGroups().add(userGroup);
      mongoTemplate.save(otherUser);
      mongoTemplate.save(friendship);
    });
  }
  */

  @ChangeSet(order = "0002", id = "removeSabeq", author = "")
  public void removeSabeq(MongoTemplate mongoTemplate) {
    // Remove sabeq
    User sabeq = mongoTemplate.findById(User.SABEQ_ID, User.class);
    mongoTemplate.remove(sabeq);

    // Remove sabeq friendships
    ArrayList<Friendship> friendshipsToBeSaved = new ArrayList<>();
    mongoTemplate.findAll(Friendship.class).stream().forEach(friendship -> {
      friendship.setFriends(friendship.getFriends().stream().filter(friend -> {
        return friend.getUserId() != null && !friend.getUserId().equals(User.SABEQ_ID);
      }).collect(Collectors.toList()));
      friendshipsToBeSaved.add(friendship);
    });
    friendshipsToBeSaved.stream().forEach(friendship -> mongoTemplate.save(friendship));

    // Remove sabeq groups
    List<Group> groupsToBeRemoved = new ArrayList<>();
    HashSet<String> groupIdsToBeRemoved = new HashSet<>();
    mongoTemplate.findAll(Group.class).stream().forEach(group -> {
      if (group != null && group.getUsersIds() != null && group.getUsersIds()
          .contains(User.SABEQ_ID)) {
        groupsToBeRemoved.add(group);
        groupIdsToBeRemoved.add(group.getId());
      }
    });
    groupsToBeRemoved.stream().forEach(group -> mongoTemplate.remove(group));

    // Remove challenges with sabeq
    List<User> usersToBeSaved = new ArrayList<>();
    mongoTemplate.findAll(User.class).stream().forEach(user -> {
      if (user != null && user.getUserChallenges() != null) {
        user.setUserChallenges(user.getUserChallenges().stream().filter(challenge -> {
          return challenge != null && challenge.getGroupId() != null && !groupIdsToBeRemoved
              .contains(challenge.getGroupId());
        }).collect(Collectors.toList()));
      }
      if (user != null && user.getUserGroups() != null) {
        user.setUserGroups(user.getUserGroups().stream().filter(userGroup -> {
          return userGroup != null && userGroup.getGroupId() != null && !groupIdsToBeRemoved
              .contains(userGroup.getGroupId());
        }).collect(Collectors.toList()));
      }
      usersToBeSaved.add(user);
    });
    usersToBeSaved.stream().forEach(user -> mongoTemplate.save(user));
  }

  @ChangeSet(order = "0003", id = "addSabeqAgain", author = "")
  public void addSabeqAgain(MongoTemplate mongoTemplate) {
    // Check the unicode displayed at https://emojipedia.org/racing-car/
    User sabeq = User.builder()
        .id(User.SABEQ_ID)
        .firstName("سابق")
        .lastName("\uD83C\uDFCE️️")
        .username("sabeq")
        .build();
    mongoTemplate.save(sabeq);

    mongoTemplate.findAll(Friendship.class).stream().forEach(friendship -> {
      // Add sabeq as a friend to this user.
      Group binaryGroup = Group.builder()
          .usersIds(Arrays.asList(friendship.getUserId(), sabeq.getId()))
          .creatorId(sabeq.getId())
          .build();
      mongoTemplate.save(binaryGroup);
      Friend sabeqAsFriend = Friend.builder()
          .userId(sabeq.getId())
          .username(sabeq.getUsername())
          .firstName(sabeq.getFirstName())
          .lastName(sabeq.getLastName())
          .isPending(false)
          .groupId(binaryGroup.getId())
          .build();
      friendship.getFriends().add(0, sabeqAsFriend);

      User otherUser;
      try {
        // Copy the user's personal challenges to a challenge between the user and sabeq.
        otherUser = mongoTemplate.findById(friendship.getUserId(), User.class);
        if (otherUser.getId() == null) {
          return;
        }
      } catch (Exception e) {
        logger.error("Couldn't find user with id: " + friendship.getUserId());
        return;
      }
      UserGroup userGroup = UserGroup.builder()
          .totalScore(0)
          .monthScore(0)
          .invitingUserId(sabeq.getId())
          .groupId(binaryGroup.getId())
          .groupName("")
          .build();
      try {
        otherUser.getPersonalChallenges().stream().forEach(personalChallenge -> {
          Challenge originalPersonalChallenge;
          try {
            originalPersonalChallenge =
                mongoTemplate.findById(personalChallenge.getId(), Challenge.class);
          } catch (Exception e) {
            logger.error("Couldn't find personal challenge with ID: " + personalChallenge.getId());
            return;
          }
          if (originalPersonalChallenge == null) {
            logger.error("Couldn't find personal challenge with ID: " + personalChallenge.getId());
            return;
          }

          originalPersonalChallenge.setGroupId(binaryGroup.getId());
          mongoTemplate.save(originalPersonalChallenge);

          // Migrate the user progress.
          originalPersonalChallenge.setSubChallenges(personalChallenge.getSubChallenges());
          if (originalPersonalChallenge.finished()) {
            userGroup.setTotalScore(userGroup.getTotalScore() + 1);
          }
          otherUser.getUserChallenges().add(originalPersonalChallenge);
        });
      } catch (Exception e) {
        logger
            .warn("Couldn't migrate personal challenges for user with ID: " + otherUser.getId(), e);
      }

      otherUser.getUserGroups().add(userGroup);
      mongoTemplate.save(otherUser);
      mongoTemplate.save(friendship);
    });
  }
}
