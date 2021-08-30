package com.azkar.services;

import com.azkar.entities.challenges.AzkarChallenge;
import com.azkar.entities.challenges.MeaningChallenge;
import com.azkar.payload.challengecontroller.responses.GetChallengesV2Response;
import com.azkar.payload.challengecontroller.responses.GetChallengesV2Response.ReturnedChallenge;
import com.azkar.repos.AzkarChallengeRepo;
import com.azkar.repos.MeaningChallengeRepo;
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
    } else {
      logger
          .error("Could not find neither an azkar challenge nor a meaning challenge.");
      return 0;
    }
  }
}
