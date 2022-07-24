package com.azkar.services;

import com.azkar.configs.AsyncConfig;
import com.azkar.entities.User;
import com.azkar.entities.challenges.AzkarChallenge;
import com.azkar.entities.challenges.MeaningChallenge;
import com.azkar.entities.challenges.MemorizationChallenge;
import com.azkar.entities.challenges.ReadingQuranChallenge;
import com.azkar.payload.challengecontroller.responses.GetChallengesV2Response.ReturnedChallenge;
import com.azkar.payload.utils.FeaturesVersions;
import com.azkar.payload.utils.VersionComparator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ChallengesService {

  private static final Logger logger = LoggerFactory.getLogger(ChallengesService.class);

  // Increasing this results in significant amount of delays in GET apiHome requests because all
  // the azkar text will be sent by the server to the client.
  private static final int MAX_RETURNED_AZKAR_CHALLENGES = 5;
  private static final int MAX_RETURNED_READING_QURAN_CHALLENGES = 5;
  private static final int MAX_RETURNED_MEANING_CHALLENGES = 5;
  private static final int MAX_RETURNED_MEMORIZATION_CHALLENGES = 5;

  @Autowired
  ReturnedChallengeComparator returnedChallengeComparator;

  @Async(value = AsyncConfig.CONTROLLERS_TASK_EXECUTOR)
  public CompletableFuture<List<ReturnedChallenge>> getAllChallenges(String apiVersion, User user) {
    List<AzkarChallenge> allUserAzkarChallenges = user.getAzkarChallenges();
    List<MeaningChallenge> allUserMeaningChallenges =
        user.getMeaningChallenges();
    List<ReadingQuranChallenge> allUserReadingQuranChallenges =
        user.getReadingQuranChallenges();
    List<MemorizationChallenge> allUserMemorizationChallenges = user.getMemorizationChallenges();

    List<AzkarChallenge> recentUserAzkarChallenges =
        allUserAzkarChallenges.subList(Math.max(0,
            allUserAzkarChallenges.size() - MAX_RETURNED_AZKAR_CHALLENGES),
            allUserAzkarChallenges.size());
    List<MeaningChallenge> recentUserMeaningChallenges =
        allUserMeaningChallenges
            .subList(Math.max(0, allUserMeaningChallenges.size() - MAX_RETURNED_MEANING_CHALLENGES),
                allUserMeaningChallenges.size());
    List<ReadingQuranChallenge> recentReadingQuranChallenges =
        allUserReadingQuranChallenges
            .subList(Math.max(0,
                allUserReadingQuranChallenges.size() - MAX_RETURNED_READING_QURAN_CHALLENGES),
                allUserReadingQuranChallenges.size());
    List<MemorizationChallenge> recentMemorizationChallenges =
        allUserMemorizationChallenges
            .subList(Math.max(0,
                allUserMemorizationChallenges.size() - MAX_RETURNED_MEMORIZATION_CHALLENGES),
                allUserMemorizationChallenges.size());

    recentUserAzkarChallenges = filterAzkarDetails(recentUserAzkarChallenges);

    List<ReturnedChallenge> challenges = new ArrayList<>();
    recentUserAzkarChallenges.forEach(azkarChallenge -> {
      challenges.add(ReturnedChallenge.builder().azkarChallenge(azkarChallenge).build());
    });
    recentUserMeaningChallenges.forEach(meaningChallenge ->
        challenges.add(ReturnedChallenge.builder().meaningChallenge(meaningChallenge).build()));

    if (apiVersion != null
        && VersionComparator.compare(apiVersion, FeaturesVersions.READING_QURAN_CHALLENGE_VERSION)
        >= 0) {
      recentReadingQuranChallenges.forEach(readingQuranChallenge ->
          challenges
              .add(ReturnedChallenge.builder().readingQuranChallenge(readingQuranChallenge)
                  .build()));
    }

    if (apiVersion != null
        && VersionComparator.compare(apiVersion, FeaturesVersions.MEMORIZATION_CHALLENGE_VERSION)
        >= 0) {
      recentMemorizationChallenges.forEach(memorizationChallenge ->
          challenges
              .add(ReturnedChallenge.builder().memorizationChallenge(memorizationChallenge)
                  .build()));
    }

    challenges.sort(returnedChallengeComparator);
    return CompletableFuture.completedFuture(challenges);
  }

  // This removes the azkar text from the to-be-returned azkar challenges. That's because of
  // their significant size and contribution to latency and because clients request every azkar
  // challenge before viewing it.
  private List<AzkarChallenge> filterAzkarDetails(List<AzkarChallenge> recentUserAzkarChallenges) {
    recentUserAzkarChallenges.stream().forEach(azkarChallenge -> {
      azkarChallenge.setSubChallenges(new ArrayList<>());
    });
    return null;
  }
}
