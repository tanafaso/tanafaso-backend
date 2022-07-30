package com.azkar.services;

import com.azkar.configs.AsyncConfig;
import com.azkar.entities.User;
import com.azkar.entities.Zekr;
import com.azkar.entities.challenges.AzkarChallenge;
import com.azkar.entities.challenges.AzkarChallenge.SubChallenge;
import com.azkar.entities.challenges.MeaningChallenge;
import com.azkar.entities.challenges.MemorizationChallenge;
import com.azkar.entities.challenges.ReadingQuranChallenge;
import com.azkar.payload.challengecontroller.responses.GetChallengesV2Response.ReturnedChallenge;
import com.azkar.payload.utils.FeaturesVersions;
import com.azkar.payload.utils.VersionComparator;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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


  /**
   * Returns the latest user created challenges respecting MAX_RETURNED_{challenge_name}_CHALLENGES
   * , puts pending ones first and then sorts them with the increasing order of expiry date.
   */
  @Async(value = AsyncConfig.CONTROLLERS_TASK_EXECUTOR)
  public CompletableFuture<List<ReturnedChallenge>> getAllChallenges(String apiVersion, User user) {
    List<AzkarChallenge> allUserAzkarChallenges = user.getAzkarChallenges();
    List<ReadingQuranChallenge> allUserReadingQuranChallenges =
        user.getReadingQuranChallenges();
    List<MeaningChallenge> allUserMeaningChallenges =
        user.getMeaningChallenges();
    List<MemorizationChallenge> allUserMemorizationChallenges = user.getMemorizationChallenges();

    List<ReturnedChallenge> challenges = new ArrayList<>();
    for (int i = 0; i < Math.min(MAX_RETURNED_AZKAR_CHALLENGES, allUserAzkarChallenges.size());
        i++) {
      challenges.add(ReturnedChallenge.builder().azkarChallenge(
          filterAzkarDetails(allUserAzkarChallenges.get(allUserAzkarChallenges.size() - 1 - i)))
          .build());
    }
    for (int i = 0; i < Math.min(MAX_RETURNED_MEANING_CHALLENGES, allUserMeaningChallenges.size());
        i++) {
      challenges.add(ReturnedChallenge.builder().meaningChallenge(
          allUserMeaningChallenges.get(allUserMeaningChallenges.size() - 1 - i))
          .build());
    }

    if (apiVersion != null
        && VersionComparator.compare(apiVersion, FeaturesVersions.READING_QURAN_CHALLENGE_VERSION)
        >= 0) {
      for (int i = 0;
          i < Math.min(MAX_RETURNED_READING_QURAN_CHALLENGES, allUserReadingQuranChallenges.size());
          i++) {
        challenges.add(ReturnedChallenge.builder().readingQuranChallenge(
            allUserReadingQuranChallenges.get(allUserReadingQuranChallenges.size() - 1 - i))
            .build());
      }
    }

    if (apiVersion != null
        && VersionComparator.compare(apiVersion, FeaturesVersions.MEMORIZATION_CHALLENGE_VERSION)
        >= 0) {
      for (int i = 0;
          i < Math.min(MAX_RETURNED_MEMORIZATION_CHALLENGES, allUserMemorizationChallenges.size());
          i++) {
        challenges.add(ReturnedChallenge.builder().memorizationChallenge(
            allUserMemorizationChallenges.get(allUserMemorizationChallenges.size() - 1 - i))
            .build());
      }
    }

    challenges.sort(new ChallengesSorter());
    return CompletableFuture.completedFuture(challenges);
  }

  private static class ChallengesSorter implements Comparator<ReturnedChallenge> {

    public boolean isFinishedOrExpired(ReturnedChallenge r) {
      return r.getAzkarChallenge() != null ?
          r.getAzkarChallenge().finished() || r.getAzkarChallenge().expired() :
          r.getMeaningChallenge() != null
              ? r.getMeaningChallenge().isFinished() || r.getMeaningChallenge().expired() :
              r.getReadingQuranChallenge() != null ?
                  r.getReadingQuranChallenge().isFinished() || r.getReadingQuranChallenge()
                      .expired() :
                  r.getMemorizationChallenge() != null ?
                      r.getMemorizationChallenge().finished() || r.getMemorizationChallenge()
                          .expired()
                      : true;
    }

    public long getExpiryDate(ReturnedChallenge r) {
      return r.getAzkarChallenge() != null ? r.getAzkarChallenge().getExpiryDate() :
          r.getMeaningChallenge() != null
              ? r.getMeaningChallenge().getExpiryDate() :
              r.getReadingQuranChallenge() != null ? r.getReadingQuranChallenge().getExpiryDate() :
                  r.getMemorizationChallenge() != null
                      ? r.getMemorizationChallenge().getExpiryDate() : 0;
    }

    @Override public int compare(ReturnedChallenge r1, ReturnedChallenge r2) {
      boolean o1Finished = isFinishedOrExpired(r1);
      boolean o2Finished = isFinishedOrExpired(r2);

      int comparisonResult;
      if (o1Finished == o2Finished) {
        comparisonResult = Long.compare(getExpiryDate(r1), getExpiryDate(r2));
      } else {
        comparisonResult = !o1Finished ? -1 : 1;
      }
      return comparisonResult;
    }
  }

  // This removes the azkar text from the to-be-returned azkar challenge. That's because of
  // their significant size and contribution to latency and because clients request every azkar
  // challenge before viewing it.
  private AzkarChallenge filterAzkarDetails(AzkarChallenge azkarChallenge) {
    return AzkarChallenge.builder()
        .motivation(azkarChallenge.getMotivation())
        .name(azkarChallenge.getName())
        .expiryDate(azkarChallenge.getExpiryDate())
        .createdAt(azkarChallenge.getCreatedAt())
        .modifiedAt(azkarChallenge.getModifiedAt())
        .usersFinished(azkarChallenge.getUsersFinished())
        .groupId(azkarChallenge.getGroupId())
        .creatingUserId(azkarChallenge.getCreatingUserId())
        .id(azkarChallenge.getId())
        .subChallenges(getEmptyOrOneSubChallengeList(azkarChallenge.finished()))
        .build();
  }

  private List<SubChallenge> getEmptyOrOneSubChallengeList(boolean finished) {
    if (finished) {
      return new ArrayList<>();
    }
    return ImmutableList.of(
        SubChallenge.builder().repetitions(1).zekr(Zekr.builder().zekr("").id(0).build()).build());
  }
}
