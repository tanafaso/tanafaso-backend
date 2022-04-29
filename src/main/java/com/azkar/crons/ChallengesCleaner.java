package com.azkar.crons;

import com.azkar.entities.User;
import com.azkar.repos.UserRepo;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Lazy(value = false)
public class ChallengesCleaner {

  private static final Logger logger = LoggerFactory.getLogger(ChallengesCleaner.class);
  private static final int USERS_BATCH_SIZE = 100;

  @Autowired
  private UserRepo userRepo;

  // Runs once every night to delete old challenges. "Old" is a bit misleading here as the
  // function will actually delete the oldest challenges for every user until every user has a
  // maximum of 30 challenges per category.
  // 0 0 0 * * * means daily at midnight. For more information, check: https://docs.spring
  // .io/spring-framework/docs/current/reference/html/integration.html#scheduling-task-scheduler
  @Scheduled(cron = "0 0 3 * * *")
  private void cleanOldChallenges() {
    logger.info("[Challenges cleaner] started!");

    long numberOfUsers = userRepo.count();
    logger.info("[Challenges cleaner] number of users to be processed: {}", numberOfUsers);
    logger.info("[Challenges cleaner] users will be processed in batches of {}", USERS_BATCH_SIZE);

    long numberOfBatches = (numberOfUsers + USERS_BATCH_SIZE - 1) / USERS_BATCH_SIZE;
    logger.info("[Challenges cleaner] number of batches to be processed: {}", numberOfBatches);

    for (int batch = 0; batch < numberOfBatches; batch++) {
      Page<User> page = userRepo.findAll(PageRequest.of(batch, USERS_BATCH_SIZE));
      List<User> modifiedUsers = page.get().map(user -> {
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

      userRepo.saveAll(modifiedUsers);

      logger.info("[Challenges cleaner] processed {}/{} batches", batch + 1, numberOfBatches);
    }

    logger.info("[Challenges cleaner] finished!");
  }
}
