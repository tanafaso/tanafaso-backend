package com.azkar.services;

import com.azkar.entities.challenges.AzkarChallenge;
import com.azkar.entities.challenges.MeaningChallenge;
import com.azkar.entities.challenges.MemorizationChallenge;
import com.azkar.entities.challenges.ReadingQuranChallenge;
import com.azkar.payload.challengecontroller.responses.GetChallengesV2Response;
import com.azkar.payload.challengecontroller.responses.GetChallengesV2Response.ReturnedChallenge;
import com.azkar.repos.AzkarChallengeRepo;
import com.azkar.repos.MeaningChallengeRepo;
import com.azkar.repos.MemorizationChallengeRepo;
import com.azkar.repos.ReadingQuranChallengeRepo;
import java.util.Comparator;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReturnedChallengeComparator implements Comparator<ReturnedChallenge> {

  private static final Logger logger = LoggerFactory.getLogger(GetChallengesV2Response.class);

  @Autowired
  AzkarChallengeRepo azkarChallengeRepo;

  @Autowired
  MeaningChallengeRepo meaningChallengeRepo;

  @Autowired
  ReadingQuranChallengeRepo readingQuranChallengeRepo;

  @Autowired
  MemorizationChallengeRepo memorizationChallengeRepo;

  @Override public int compare(ReturnedChallenge o1, ReturnedChallenge o2) {
    return Long.compare(getModifiedDateFromDbForChallenge(o2),
        getModifiedDateFromDbForChallenge(o1));
  }

  private long getModifiedDateFromDbForChallenge(ReturnedChallenge o) {
    if (o.getAzkarChallenge() != null) {
      Optional<AzkarChallenge> azkarChallengeFromDb =
          azkarChallengeRepo.findById(o.getAzkarChallenge().getId());
      if (!azkarChallengeFromDb.isPresent()) {
        logger
            .error("Could not find azkar challenge with ID {} in DB.",
                o.getAzkarChallenge().getId());
        return 0;
      }
      return azkarChallengeFromDb.get().getModifiedAt();
    } else if (o.getMeaningChallenge() != null) {
      Optional<MeaningChallenge> meaningChallengeFromDb =
          meaningChallengeRepo.findById(o.getMeaningChallenge().getId());
      if (!meaningChallengeFromDb.isPresent()) {
        logger.error("Could not find meaning challenge with ID {} in DB",
            o.getMeaningChallenge().getId());
        return 0;
      }
      return meaningChallengeFromDb.get().getModifiedAt();
    } else if (o.getReadingQuranChallenge() != null) {
      Optional<ReadingQuranChallenge> readingQuranChallengeFromDb =
          readingQuranChallengeRepo.findById(o.getReadingQuranChallenge().getId());
      if (!readingQuranChallengeFromDb.isPresent()) {
        logger.error("Could not find reading Quran challenge with ID {} in DB",
            o.getReadingQuranChallenge().getId());
        return 0;
      }
      return readingQuranChallengeFromDb.get().getModifiedAt();
    } else if (o.getMemorizationChallenge() != null) {
      Optional<MemorizationChallenge> memorizationChallengeFromDb =
          memorizationChallengeRepo.findById(o.getMemorizationChallenge().getId());
      if (!memorizationChallengeFromDb.isPresent()) {
        logger.error("Could not find memorization challenge with ID {} in DB",
            o.getMemorizationChallenge().getId());
        return 0;
      }
      return memorizationChallengeFromDb.get().getModifiedAt();
    } else {
      logger
          .error("Could not find neither an azkar challenge nor a meaning challenge.");
      return 0;
    }
  }
}
