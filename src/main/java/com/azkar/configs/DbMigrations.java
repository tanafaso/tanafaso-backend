package com.azkar.configs;

import com.azkar.entities.Friendship;
import com.azkar.entities.Friendship.Friend;
import com.azkar.entities.Group;
import com.azkar.entities.PubliclyAvailableFemaleUser;
import com.azkar.entities.PubliclyAvailableMaleUser;
import com.azkar.entities.User;
import com.azkar.entities.User.UserGroup;
import com.azkar.entities.challenges.AzkarChallenge;
import com.azkar.payload.utils.UserScore;
import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Service;

@Configuration
@ChangeLog
@EnableMongoRepositories(basePackages = {"com.azkar.repos"})
@Service
public class DbMigrations {

  private static final Logger logger = LoggerFactory.getLogger(DbMigrations.class);

  // This change set has failed because a lot of corner cases weren't handled which led to leaving
  // the database in an inconsistent state. Th next change set aims to remove everything about
  // sabeq.
  @ChangeSet(order = "0001", id = "addSabeq", author = "")
  public void failedAddSabeq(MongoTemplate mongoTemplate) {
    try {
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
          AzkarChallenge originalPersonalChallenge =
              mongoTemplate.findById(personalChallenge.getId(), AzkarChallenge.class);
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
    } catch (Exception expectedException) {
    }
  }

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
          AzkarChallenge originalPersonalChallenge;
          try {
            originalPersonalChallenge =
                mongoTemplate.findById(personalChallenge.getId(), AzkarChallenge.class);
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

  @ChangeSet(order = "0004", id = "populateFriendsScores", author = "")
  public void populateFriendsScores(MongoTemplate mongoTemplate) {
    List<Friendship> friendshipsToBeSaved = new ArrayList<>();
    mongoTemplate.findAll(Friendship.class).stream().forEach(friendship -> {
      List<Friend> reformedFriends = new ArrayList<>();
      friendship.getFriends().stream().forEach(friend -> {
        updateFriendScore(mongoTemplate, friendship.getUserId(), friend);
        reformedFriends.add(friend);
      });
      friendship.setFriends(reformedFriends);
      friendshipsToBeSaved.add(friendship);
    });

    friendshipsToBeSaved.stream().forEach(friendship -> mongoTemplate.save(friendship));
  }

  @ChangeSet(order = "0005", id = "createEmptyFriendshipForSabeq", author = "")
  public void createEmptyFriendshipForSabeq(MongoTemplate mongoTemplate) {
    Friendship sabeqFriendship = Friendship.builder()
        .userId(User.SABEQ_ID)
        .friends(new ArrayList<>())
        .id(new ObjectId().toString())
        .build();

    mongoTemplate.findAll(User.class).stream().forEach(user -> {
      if (!user.getId().equals(User.SABEQ_ID)) {
        sabeqFriendship.getFriends().add(Friend.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .isPending(false)
            .userTotalScore(0)
            .friendTotalScore(0)
            .groupId(null)
            .build()
        );
      }
    });
    mongoTemplate.save(sabeqFriendship);
  }

  @ChangeSet(order = "0006", id = "addOldPersonalChallengesToSabeqScore", author = "")
  public void addOldPersonalChallengesToSabeqScore(MongoTemplate mongoTemplate) {
    mongoTemplate.findAll(Friendship.class).stream().forEach(friendship -> {
      if (friendship.getFriends().size() == 0 || !friendship.getFriends().get(0).getUserId()
          .equals(User.SABEQ_ID)) {
        logger.warn("Found a friendship of size 0. Its ID is %s", friendship.getId());
        return;
      }
      logger.info("Migrating old personal challenge score for user with ID: %s",
          friendship.getUserId());

      Friend saqbeqAsAFriend = friendship.getFriends().get(0);
      User user;
      try {
        // Copy the user's personal challenges to a challenge between the user and sabeq.
        user = mongoTemplate.findById(friendship.getUserId(), User.class);
        if (user.getId() == null) {
          logger.warn("Found a user with null ID");
          return;
        }
      } catch (Exception e) {
        logger.error("Couldn't find user with id: " + friendship.getUserId());
        return;
      }

      try {
        user.getPersonalChallenges().stream().forEach(personalChallenge -> {
          if (!personalChallenge.finished()) {
            return;
          }
          AzkarChallenge originalPersonalChallenge = null;
          try {
            originalPersonalChallenge =
                mongoTemplate.findById(personalChallenge.getId(), AzkarChallenge.class);
          } catch (Exception e) {
            logger.info("Mongotemplate failed to find an original challenge for user: %s",
                user.getId());
          }

          if (originalPersonalChallenge != null) {
            // not old
            return;
          }
          logger.info("Found an old challenge for user with ID: %s", user.getId());

          long oldScore = saqbeqAsAFriend.getUserTotalScore();
          saqbeqAsAFriend.setUserTotalScore(oldScore + 1);

          UserGroup userAndSabeqGroup = user.getUserGroups().stream()
              .filter(userGroup -> userGroup.getGroupId().equals(saqbeqAsAFriend.getGroupId()))
              .findFirst()
              .orElse(null);
          if (userAndSabeqGroup == null) {
            logger.warn("Couldn't find user group with sabeq for user with ID: %s", user.getId());
            return;
          }
          userAndSabeqGroup.setTotalScore((int) (oldScore + 1));
        });
        mongoTemplate.save(friendship);
        mongoTemplate.save(user);
      } catch (Exception e) {
        logger.warn("Something wrong happened while updating score for user with ID: %s",
            user.getId());
      }
    });
  }

  @ChangeSet(order = "0007", id = "populateUserAzkarChallenges", author = "")
  public void populateUserAzkarChallenges(MongoTemplate mongoTemplate) {
    mongoTemplate.findAll(User.class).stream().forEach(user -> {
      user.setAzkarChallenges(user.getUserChallenges());
      // Noted later: that was not safe as we shouldn't be modifying the source of the stream
      // within the stream function.
      mongoTemplate.save(user);
    });
  }

  @ChangeSet(order = "0008", id = "populateFinishedChallengesCount", author = "")
  public void populateFinishedChallengesCount(MongoTemplate mongoTemplate) {
    List<User> modifiedUsers = mongoTemplate.findAll(User.class).stream().map(user -> {
      int finishedAzkarChallengesCount =
          (int) user.getAzkarChallenges().stream()
              .filter(azkarChallenge -> azkarChallenge.finished()).count();
      user.setFinishedAzkarChallengesCount(finishedAzkarChallengesCount);

      int finishedReadingQuranChallengesCount =
          (int) user.getReadingQuranChallenges().stream()
              .filter(readingQuranChallenge -> readingQuranChallenge.isFinished()).count();
      user.setFinishedReadingQuranChallengesCount(finishedReadingQuranChallengesCount);

      int finishedMeaningChallengesCount =
          (int) user.getMeaningChallenges().stream()
              .filter(meaningChallenge -> meaningChallenge.isFinished()).count();
      user.setFinishedMeaningChallengesCount(finishedMeaningChallengesCount);

      logger.info("Populating finished challenges count [azkar: {}, reading: {}, meaning: {}] for "
              + "user: {}", finishedAzkarChallengesCount, finishedReadingQuranChallengesCount,
          finishedMeaningChallengesCount, user.getUsername());
      return user;
    }).collect(Collectors.toList());

    modifiedUsers.stream().forEach(user -> mongoTemplate.save(user));
  }

  @ChangeSet(order = "0009", id = "cleanUpOldChallenges", author = "")
  public void cleanUpOldChallenges(MongoTemplate mongoTemplate) {
    List<User> modifiedUsers = mongoTemplate.findAll(User.class).stream().map(user -> {
      user.setPersonalChallenges(new ArrayList<>());
      user.setUserChallenges(new ArrayList<>());

      user.setAzkarChallenges(user.getAzkarChallenges().subList(
          Math.max(0, user.getAzkarChallenges().size() - 100),
          user.getAzkarChallenges().size()));

      user.setReadingQuranChallenges(user.getReadingQuranChallenges().subList(
          Math.max(0, user.getReadingQuranChallenges().size() - 100),
          user.getReadingQuranChallenges().size()));

      user.setMeaningChallenges(user.getMeaningChallenges().subList(
          Math.max(0, user.getMeaningChallenges().size() - 100),
          user.getMeaningChallenges().size()));
      return user;
    }).collect(Collectors.toList());

    modifiedUsers.forEach(user -> mongoTemplate.save(user));
  }

  // In one bad release, we have used com/sun/tools/javac/util/List and that affected some new
  // users using UserService in such a way that those users were actually created but no
  // friendships or test challenges were added to them.
  // The bad dependency were solved in https://github.com/tanafaso/tanafaso-backend/pull/351
  @ChangeSet(order = "0010", id = "cleanUpUsersWithoutFriendships", author = "")
  public void cleanUpUsersWithoutFriendships(MongoTemplate mongoTemplate) {
    Friendship sabeqFriendship =
        mongoTemplate.findAll(Friendship.class).stream()
            .filter(friendship -> friendship.getUserId().equals(User.SABEQ_ID)).findFirst().get();

    List<User> usersToBeRemoved = new ArrayList<>();
    mongoTemplate.findAll(User.class).stream().forEach(user -> {
      if (!mongoTemplate.findAll(Friendship.class).stream()
          .anyMatch(friendship -> friendship.getUserId().equals(user.getId()))) {
        logger.info("Will remove user with ID {} and name {} and username {}", user.getId(),
            user.getFirstName(), user.getUsername());
        usersToBeRemoved.add(user);

        if (sabeqFriendship.getFriends().stream()
            .anyMatch(friend -> friend.getUserId().equals(user.getId()))) {
          logger.error("Found friendship with sabeq for user with ID: {}", user.getId());
        }
      }
    });
    logger.info("Number of users to be removed {}", usersToBeRemoved.size());
    usersToBeRemoved.stream().forEach(user -> mongoTemplate.remove(user));
  }

  // That is a follow-up from the last migration.
  @ChangeSet(order = "0011", id = "cleanUpNonExistingPubliclyAvailableUsers", author = "")
  public void cleanUpNonExistingPubliclyAvailableUsers(MongoTemplate mongoTemplate) {
    Set<String> existingUserIds = new HashSet<>();
    mongoTemplate.findAll(User.class).stream().forEach(user -> existingUserIds.add(user.getId()));

    List<PubliclyAvailableMaleUser> publiclyAvailableMaleUsersToBeDeleted = new ArrayList<>();
    List<PubliclyAvailableFemaleUser> publiclyAvailableFemaleUsersToBeDeleted = new ArrayList<>();
    mongoTemplate.findAll(PubliclyAvailableMaleUser.class).stream()
        .forEach(publiclyAvailableMaleUser -> {
          if (!existingUserIds.contains(publiclyAvailableMaleUser.getUserId())) {
            logger.info("Will delete publicly available male user with ID: {}",
                publiclyAvailableMaleUser.getUserId(), publiclyAvailableMaleUser.getFirstName());
            publiclyAvailableMaleUsersToBeDeleted.add(publiclyAvailableMaleUser);
          }
        });
    mongoTemplate.findAll(PubliclyAvailableFemaleUser.class).stream()
        .forEach(publiclyAvailableFemaleUser -> {
          if (!existingUserIds.contains(publiclyAvailableFemaleUser.getUserId())) {
            logger.info("Will delete publicly available female user with ID: {}",
                publiclyAvailableFemaleUser.getUserId(),
                publiclyAvailableFemaleUser.getFirstName());
            publiclyAvailableFemaleUsersToBeDeleted.add(publiclyAvailableFemaleUser);
          }
        });
    publiclyAvailableMaleUsersToBeDeleted.stream().forEach(
        publiclyAvailableMaleUser -> mongoTemplate
            .remove(publiclyAvailableMaleUser));
    publiclyAvailableFemaleUsersToBeDeleted.stream().forEach(
        publiclyAvailableFemaleUser -> mongoTemplate
            .remove(publiclyAvailableFemaleUser));
  }

  @ChangeSet(order = "0012", id = "cleanUpOldChallenges2", author = "")
  public void cleanUpOldChallenges2(MongoTemplate mongoTemplate) {
    List<User> modifiedUsers = mongoTemplate.findAll(User.class).stream().map(user -> {
      user.setAzkarChallenges(user.getAzkarChallenges().subList(
          Math.max(0, user.getAzkarChallenges().size() - 30),
          user.getAzkarChallenges().size()));

      user.setReadingQuranChallenges(user.getReadingQuranChallenges().subList(
          Math.max(0, user.getReadingQuranChallenges().size() - 30),
          user.getReadingQuranChallenges().size()));

      user.setMeaningChallenges(user.getMeaningChallenges().subList(
          Math.max(0, user.getMeaningChallenges().size() - 30),
          user.getMeaningChallenges().size()));

      user.setMemorizationChallenges(user.getMemorizationChallenges().subList(
          Math.max(0, user.getMemorizationChallenges().size() - 30),
          user.getMemorizationChallenges().size()));
      return user;
    }).collect(Collectors.toList());

    modifiedUsers.forEach(user -> mongoTemplate.save(user));
  }

  @ChangeSet(order = "0013", id = "cleanUpOldChallenges3", author = "")
  public void cleanUpOldChallenges3(MongoTemplate mongoTemplate) {
    List<User> modifiedUsers = mongoTemplate.findAll(User.class).stream().map(user -> {
      user.setAzkarChallenges(user.getAzkarChallenges().subList(
          Math.max(0, user.getAzkarChallenges().size() - 30),
          user.getAzkarChallenges().size()));

      user.setReadingQuranChallenges(user.getReadingQuranChallenges().subList(
          Math.max(0, user.getReadingQuranChallenges().size() - 30),
          user.getReadingQuranChallenges().size()));

      user.setMeaningChallenges(user.getMeaningChallenges().subList(
          Math.max(0, user.getMeaningChallenges().size() - 30),
          user.getMeaningChallenges().size()));

      user.setMemorizationChallenges(user.getMemorizationChallenges().subList(
          Math.max(0, user.getMemorizationChallenges().size() - 30),
          user.getMemorizationChallenges().size()));
      return user;
    }).collect(Collectors.toList());

    modifiedUsers.forEach(user -> mongoTemplate.save(user));
  }

  @ChangeSet(order = "0014", id = "cleanUpUserGroups", author = "")
  public void cleanUpUserGroups(MongoTemplate mongoTemplate) {
    List<User> modifiedUsers = mongoTemplate.findAll(User.class).stream().map(user -> {
      user.setUserGroups(new ArrayList<>());
      return user;
    }).collect(Collectors.toList());

    modifiedUsers.forEach(user -> mongoTemplate.save(user));
  }


  private void updateFriendScore(MongoTemplate mongoTemplate, String userId, Friend friend) {
    User user = mongoTemplate.findById(userId, User.class);
    User friendUser = mongoTemplate.findById(friend.getUserId(), User.class);
    List<UserScore> userScores = getFriendsScores(mongoTemplate, user, friendUser);
    friend.setUserTotalScore(userScores.get(0).getTotalScore());
    friend.setFriendTotalScore(userScores.get(1).getTotalScore());
  }

  // Accumulates the scores of the two users in all of the groups they are both members in.
  private List<UserScore> getFriendsScores(MongoTemplate mongoTemplate, User user1, User user2) {
    // Create array so as to be able to update the inner elements in lambda.
    int[] usersScores = {0, 0};
    mongoTemplate.findAll(Group.class).stream().filter(
        grp -> (grp.getUsersIds().contains(user1.getId()) && grp.getUsersIds()
            .contains(user2.getId())))
        .forEach(grp -> {
          Optional<UserScore> user1ScoreInGrp =
              getUserScoreInGroup(mongoTemplate, user1.getId(), grp);
          Optional<UserScore> user2ScoreInGrp =
              getUserScoreInGroup(mongoTemplate, user2.getId(), grp);
          user1ScoreInGrp.ifPresent(userScore -> usersScores[0] += userScore.getTotalScore());
          user2ScoreInGrp.ifPresent(userScore -> usersScores[1] += userScore.getTotalScore());
        });
    UserScore userScore1 = UserScore.builder()
        .username(user1.getUsername())
        .firstName(user1.getFirstName())
        .lastName(user1.getLastName())
        .totalScore(usersScores[0])
        .build();
    UserScore userScore2 = UserScore.builder()
        .username(user2.getUsername())
        .firstName(user2.getFirstName())
        .lastName(user2.getLastName())
        .totalScore(usersScores[1])
        .build();
    return ImmutableList.of(userScore1, userScore2);
  }

  private Optional<UserScore> getUserScoreInGroup(MongoTemplate mongoTemplate, String userId,
      Group group) {
    User user = mongoTemplate.findById(userId, User.class);
    if (user == null) {
      return Optional.empty();
    }

    Optional<UserGroup> userGroup =
        user.getUserGroups().stream()
            .filter(userGroup1 -> userGroup1.getGroupId().equals(group.getId())).findFirst();
    if (!userGroup.isPresent()) {
      return Optional.empty();
    }

    return Optional.of(
        UserScore.builder().firstName(user.getFirstName()).lastName(user.getLastName())
            .username(user.getUsername())
            .totalScore(userGroup.get().getTotalScore()).build());
  }
}
