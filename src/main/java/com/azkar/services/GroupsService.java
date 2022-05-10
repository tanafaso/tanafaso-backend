package com.azkar.services;

import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.repos.GroupRepo;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GroupsService {

  @Autowired
  GroupRepo groupRepo;

  public List<Group> getGroups(User user) {
    // Group IDs in use are the groups in which one of the user challenges belong to. Note that
    // old challenges are deleted periodically.
    HashSet<String> groupIdsInUse = new HashSet<>();
    groupIdsInUse.addAll(
        user.getAzkarChallenges().stream().map(azkarChallenge -> azkarChallenge.getGroupId())
            .collect(
                Collectors.toList()));
    groupIdsInUse.addAll(
        user.getReadingQuranChallenges().stream()
            .map(readingQuranChallenge -> readingQuranChallenge.getGroupId())
            .collect(
                Collectors.toList()));
    groupIdsInUse.addAll(
        user.getMeaningChallenges().stream().map(meaningChallenge -> meaningChallenge.getGroupId())
            .collect(
                Collectors.toList()));
    groupIdsInUse.addAll(
        user.getMemorizationChallenges().stream()
            .map(memorizationChallenge -> memorizationChallenge.getGroupId())
            .collect(
                Collectors.toList()));

    return groupIdsInUse.stream().map(groupId -> groupRepo.findById(groupId).get())
        .collect(Collectors.toList());
  }
}
