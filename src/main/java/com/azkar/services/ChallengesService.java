package com.azkar.services;

import com.azkar.entities.User;
import com.azkar.entities.challenges.AzkarChallenge;
import com.azkar.entities.challenges.MeaningChallenge;
import com.azkar.entities.challenges.ReadingQuranChallenge;
import com.azkar.payload.challengecontroller.responses.GetChallengesV2Response.ReturnedChallenge;
import com.azkar.payload.utils.FeaturesVersions;
import com.azkar.payload.utils.VersionComparator;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChallengesService {

  private static final int MAX_RETURNED_AZKAR_CHALLENGES = 20;
  private static final int MAX_RETURNED_READING_QURAN_CHALLENGES = 5;
  private static final int MAX_RETURNED_MEANING_CHALLENGES = 5;

  @Autowired
  ReturnedChallengeComparator returnedChallengeComparator;

  public List<ReturnedChallenge> getAllChallenges(String apiVersion, User user) {
    List<AzkarChallenge> allUserAzkarChallenges = user.getAzkarChallenges();
    List<MeaningChallenge> allUserMeaningChallenges =
        user.getMeaningChallenges();
    List<ReadingQuranChallenge> allUserReadingQuranChallenges =
        user.getReadingQuranChallenges();

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

    challenges.sort(returnedChallengeComparator);
    return challenges;
  }
}
