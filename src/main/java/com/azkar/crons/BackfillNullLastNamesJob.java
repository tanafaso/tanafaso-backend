package com.azkar.crons;

import com.azkar.entities.Friendship;
import com.azkar.entities.Friendship.Friend;
import com.azkar.entities.PubliclyAvailableFemaleUser;
import com.azkar.entities.PubliclyAvailableMaleUser;
import com.azkar.entities.User;
import com.azkar.repos.FriendshipRepo;
import com.azkar.repos.PubliclyAvailableFemaleUsersRepo;
import com.azkar.repos.PubliclyAvailableMaleUsersRepo;
import com.azkar.repos.UserRepo;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component public class BackfillNullLastNamesJob implements ApplicationRunner {

  private static final Logger logger = LoggerFactory.getLogger(BackfillNullLastNamesJob.class);
  private static final int READ_BATCH_SIZE = 100;

  @Autowired
  private UserRepo userRepo;

  @Autowired
  private FriendshipRepo friendshipRepo;

  @Autowired
  private PubliclyAvailableMaleUsersRepo publiclyAvailableMaleUsersRepo;

  @Autowired
  private PubliclyAvailableFemaleUsersRepo publiclyAvailableFemaleUsersRepo;

  @Autowired
  private ApplicationContext appContext;

  @Value("${backfill-null-lastnames-job-run-mode}")
  public boolean jobMode;

  // A cleanup after the fix https://github.com/tanafaso/tanafaso-backend/pull/465.
  @Override public void run(ApplicationArguments args) throws Exception {
    if (!jobMode) {
      logger.info(
          "[Backfill null lastnames] skipping as the application is not running in job mode");
      return;
    }

    backfillUsers();
    backfillFriendships();
    backfillPubliclyAvailableMales();
    backfillPublicklyAvailableFemales();

    logger.info("[Backfill null lastnames] finished all backfilles!");

    SpringApplication.exit(appContext, () -> 0);
  }

  public void backfillUsers() {
    logger.info("[Backfill null lastnames of users] started!");

    long numberOfUsers = userRepo.count();
    logger.info("[Backfill null lastnames of users] number of users to be processed: {}",
        numberOfUsers);
    logger.info("[Backfill null lastnames of users] users will be processed in batches of {}",
        READ_BATCH_SIZE);

    long numberOfBatches = (numberOfUsers + READ_BATCH_SIZE - 1) / READ_BATCH_SIZE;
    logger.info("[Backfill null lastnames of users] number of batches to be processed: {}",
        numberOfBatches);

    AtomicInteger backfilledUsernames = new AtomicInteger();
    for (int batch = 0; batch < numberOfBatches; batch++) {
      Page<User> page = userRepo.findAll(PageRequest.of(batch, READ_BATCH_SIZE));
      List<User> modifiedUsers = page.get().map(user -> {
        if (user.getLastName() == null) {
          logger.info("[Backfill null lastnames of users] backfilled lastname of user {}",
              user.getUsername());
          user.setLastName("");
          backfilledUsernames.incrementAndGet();
        }
        return user;
      }).collect(Collectors.toList());

      userRepo.saveAll(modifiedUsers);

      logger.info("[Backfill null lastnames of users] processed {}/{} batches", batch + 1,
          numberOfBatches);
    }
    logger.info("[Backfill null lastnames of users] backfilled lastnames of {} users",
        backfilledUsernames);
  }

  public void backfillFriendships() {
    logger.info("[Backfill Null lastnames of friendships] started!");

    long numberOfFriendships = friendshipRepo.count();
    logger.info(
        "[Backfill Null lastnames of friendships] number of friendships to be processed: {}",
        numberOfFriendships);
    logger.info(
        "[Backfill Null lastnames of friendships] friendships will be processed in batches of {}",
        READ_BATCH_SIZE);

    long numberOfBatches = (numberOfFriendships + READ_BATCH_SIZE - 1) / READ_BATCH_SIZE;
    logger.info("[Backfill Null lastnames of friendships] number of batches to be processed: {}",
        numberOfBatches);

    AtomicInteger cleanedFriendsCounter = new AtomicInteger();
    for (int batch = 0; batch < numberOfBatches; batch++) {
      Page<Friendship> page = friendshipRepo.findAll(PageRequest.of(batch, READ_BATCH_SIZE));

      List<Friendship> modifiedFriendships = page.get().map(friendship -> {

        AtomicInteger friendsWithNullLastnameCounter = new AtomicInteger();
        List<Friend> cleanedFriendships = friendship.getFriends().stream().map(friend -> {
          if (friend.getLastName() == null) {
            friend.setLastName("");
            friendsWithNullLastnameCounter.incrementAndGet();
          }
          return friend;
        }).collect(Collectors.toList());

        if (friendsWithNullLastnameCounter.get() != 0) {
          friendship.setFriends(cleanedFriendships);
          logger.info(
              "[Backfill Null lastnames of friendships] cleaned lastnames of {} friends for "
                  + "user {}", friendsWithNullLastnameCounter, friendship.getUserId());
        }
        cleanedFriendsCounter.addAndGet(friendsWithNullLastnameCounter.get());
        return friendship;
      }).collect(Collectors.toList());

      friendshipRepo.saveAll(modifiedFriendships);

      logger.info("[Backfill Null lastnames of friendships] processed {}/{} batches", batch + 1,
          numberOfBatches);
    }
  }

  public void backfillPubliclyAvailableMales() {
    logger.info("[Backfill null lastnames of public males] started!");

    long numberOfPubliclyAvailableMales = publiclyAvailableMaleUsersRepo.count();
    logger.info(
        "[Backfill null lastnames of public males] number of public males to be processed: {}",
        numberOfPubliclyAvailableMales);
    logger.info(
        "[Backfill null lastnames of public males] public males will be processed in batches of {}",
        READ_BATCH_SIZE);

    long numberOfBatches = (numberOfPubliclyAvailableMales + READ_BATCH_SIZE - 1) / READ_BATCH_SIZE;
    logger.info("[Backfill null lastnames of public males] number of batches to be processed: {}",
        numberOfBatches);

    AtomicInteger backfilledPubliclyAvaialbleMales = new AtomicInteger();
    for (int batch = 0; batch < numberOfBatches; batch++) {
      Page<PubliclyAvailableMaleUser> page =
          publiclyAvailableMaleUsersRepo.findAll(PageRequest.of(batch, READ_BATCH_SIZE));
      List<PubliclyAvailableMaleUser> modifiedUsers = page.get().map(user -> {
        if (user.getLastName() == null) {
          user.setLastName("");
          logger.info("[Backfill null lastnames of public males] backfilled lastname of user {}",
              user.getUserId());
          backfilledPubliclyAvaialbleMales.incrementAndGet();
        }
        return user;
      }).collect(Collectors.toList());

      publiclyAvailableMaleUsersRepo.saveAll(modifiedUsers);

      logger.info("[Backfill null lastnames of public males] processed {}/{} batches", batch + 1,
          numberOfBatches);
    }
    logger.info("[Backfill null lastnames of public males] backfilled lastnames of {} users",
        backfilledPubliclyAvaialbleMales);
  }

  public void backfillPublicklyAvailableFemales() {
    logger.info("[Backfill null lastnames of public females] started!");

    long numberOfPubliclyAvailableFemales = publiclyAvailableFemaleUsersRepo.count();
    logger.info(
        "[Backfill null lastnames of public females] number of public females to be processed: {}",
        numberOfPubliclyAvailableFemales);
    logger.info(
        "[Backfill null lastnames of public females] public females will be processed in batches of"
            + " {}", READ_BATCH_SIZE);

    long numberOfBatches =
        (numberOfPubliclyAvailableFemales + READ_BATCH_SIZE - 1) / READ_BATCH_SIZE;
    logger.info("[Backfill null lastnames of public females] number of batches to be processed: {}",
        numberOfBatches);

    AtomicInteger backfilledPubliclyAvaialbleFemales = new AtomicInteger();
    for (int batch = 0; batch < numberOfBatches; batch++) {
      Page<PubliclyAvailableFemaleUser> page =
          publiclyAvailableFemaleUsersRepo.findAll(PageRequest.of(batch, READ_BATCH_SIZE));
      List<PubliclyAvailableFemaleUser> modifiedUsers = page.get().map(user -> {
        if (user.getLastName() == null) {
          user.setLastName("");
          logger.info("[Backfill null lastnames of public females] backfilled lastname of user {}",
              user.getUserId());
          backfilledPubliclyAvaialbleFemales.incrementAndGet();
        }
        return user;
      }).collect(Collectors.toList());

      publiclyAvailableFemaleUsersRepo.saveAll(modifiedUsers);

      logger.info("[Backfill null lastnames of public females] processed {}/{} batches", batch + 1,
          numberOfBatches);
    }
    logger.info("[Backfill null lastnames of public females] backfilled lastnames of {} users",
        backfilledPubliclyAvaialbleFemales);
  }
}