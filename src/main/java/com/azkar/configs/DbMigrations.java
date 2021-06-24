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
import java.util.Arrays;
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

  @ChangeSet(order = "0001", id = "addSabeq", author = "")
  public void addSabeq(MongoTemplate mongoTemplate) {
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
}
