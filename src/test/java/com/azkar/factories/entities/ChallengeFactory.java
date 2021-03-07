package com.azkar.factories.entities;

import com.azkar.entities.Challenge;
import com.azkar.entities.Challenge.SubChallenge;
import com.google.common.collect.ImmutableList;
import java.time.Instant;

public class ChallengeFactory {

  public static final String CHALLENGE_NAME_BASE = "challenge_name";
  public final static String CHALLENGE_MOTIVATION = "challenge_motivation";
  public final static long EXPIRY_DATE_OFFSET = 60 * 60;
  private static int challengesRequested = 0;

  public static SubChallenge subChallenge1() {
    return SubChallenge.builder()
        .zekrId("1")
        .zekr("zekr")
        .repetitions(3)
        .build();
  }

  public static SubChallenge subChallenge2() {
    return SubChallenge.builder()
        .zekrId("2")
        .zekr("zekr2")
        .repetitions(5)
        .build();
  }

  public static Challenge getNewChallenge(String groupId) {
    return getNewChallenge(/* namePrefix= */ "", groupId);
  }

  public static Challenge getNewChallenge(String namePrefix, String groupId) {
    long expiryDate = Instant.now().getEpochSecond() + EXPIRY_DATE_OFFSET;
    String challengeFullName = namePrefix + CHALLENGE_NAME_BASE + ++challengesRequested;
    return Challenge.builder()
        .id(challengeFullName)
        .name(challengeFullName)
        .motivation(CHALLENGE_MOTIVATION)
        .expiryDate(expiryDate)
        .subChallenges(ImmutableList.of(subChallenge1(), subChallenge2()))
        .groupId(groupId)
        .build();
  }
}
