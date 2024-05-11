package com.azkar.crons;

import com.azkar.entities.Friendship;
import com.azkar.entities.Friendship.Friend;
import com.azkar.repos.FriendshipRepo;
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

@Component
public class FriendshipEmptyGroupIdCleanerJob implements ApplicationRunner {

  private static final Logger logger =
      LoggerFactory.getLogger(FriendshipEmptyGroupIdCleanerJob.class);
  private static final int FRIENDSHIPS_BATCH_SIZE = 100;
  @Value("${friendship-empty-group-id-cleaner-job-run-mode}")
  public boolean jobMode;
  @Autowired
  private FriendshipRepo friendshipRepo;
  @Autowired
  private ApplicationContext appContext;

  // This is a cleanup after the fix:
  // https://github.com/tanafaso/tanafaso-backend/pull/463.
  @Override
  public void run(ApplicationArguments args) throws Exception {
    if (!jobMode) {
      logger.info(
          "[Empty group ID cleaner] skipping as the application is not running in job mode");
      return;
    }

    logger.info("[Empty group ID cleaner] started!");

    long numberOfFriendships = friendshipRepo.count();
    logger.info("[Empty group ID cleaner] number of friendships to be processed: {}",
        numberOfFriendships);
    logger.info("[Empty group ID cleaner] friendships will be processed in batches of {}",
        FRIENDSHIPS_BATCH_SIZE);

    long numberOfBatches =
        (numberOfFriendships + FRIENDSHIPS_BATCH_SIZE - 1) / FRIENDSHIPS_BATCH_SIZE;
    logger.info("[Empty group ID cleaner] number of batches to be processed: {}", numberOfBatches);

    AtomicInteger cleanedFriendshps = new AtomicInteger();
    for (int batch = 0; batch < numberOfBatches; batch++) {
      Page<Friendship> page = friendshipRepo.findAll(PageRequest.of(batch, FRIENDSHIPS_BATCH_SIZE));

      List<Friendship> modifiedFriendships = page.get().map(friendship -> {
        int friendsCountBeforeCleaning = friendship.getFriends().size();

        List<Friend> cleanedFriendships =
            friendship.getFriends().stream().filter(friend -> friend.getGroupId() != null)
                .collect(Collectors.toList());

        int friendsCountAfterCleaning = cleanedFriendships.size();
        int cleanedFriendshipsCount = friendsCountBeforeCleaning - friendsCountAfterCleaning;
        if (cleanedFriendshipsCount != 0) {
          friendship.setFriends(cleanedFriendships);
          logger.info("[Empty group ID cleaner] cleaned {} friendships for user {}",
              cleanedFriendshipsCount, friendship.getUserId());
        }
        cleanedFriendshps.addAndGet((cleanedFriendshipsCount));
        return friendship;
      }).collect(Collectors.toList());

      friendshipRepo.saveAll(modifiedFriendships);

      logger.info("[Empty group ID cleaner] processed {}/{} batches", batch + 1, numberOfBatches);
    }
    SpringApplication.exit(appContext, () -> 0);
  }
}