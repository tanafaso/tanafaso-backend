package com.azkar.controllers;

import com.azkar.entities.Challenge;
import com.azkar.entities.User;
import com.azkar.entities.User.ChallengeProgress;
import com.azkar.entities.User.SubChallengeProgress;

public class ChallengeControllerUtil {

  public static void addChallengeToUser(User user, Challenge challenge) {
    ChallengeProgress challengeProgress = ChallengeProgress.builder()
        .challengeId(challenge.getId())
        .subChallenges(
            SubChallengeProgress.fromSubChallengesCollection(challenge.getSubChallenges()))
        .groupId(challenge.getGroupId())
        .build();
    user.getChallengesProgress().add(challengeProgress);
  }
}
