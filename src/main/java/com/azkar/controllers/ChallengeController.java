package com.azkar.controllers;

import com.azkar.entities.Challenge;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.payload.challengecontroller.AddChallengeRequest;
import com.azkar.payload.challengecontroller.AddChallengeResponse;
import com.azkar.repos.ChallengeRepo;
import com.azkar.repos.GroupRepo;
import com.azkar.repos.UserRepo;
import com.google.common.collect.ImmutableList;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChallengeController {
  @Autowired
  UserRepo userRepo;

  @Autowired
  GroupRepo groupRepo;

  @Autowired
  ChallengeRepo challengeRepo;

  @PostMapping(path = "/challenge", consumes = "application/json", produces = "application/json")
  public AddChallengeResponse addChallenge(@RequestBody AddChallengeRequest request) {
    AddChallengeResponse response = new AddChallengeResponse();
    Challenge challenge = Challenge.builder()
        .name(request.getName())
        .motivation(request.getMotivation())
        .expiryDate(parseDate(request.getExpiryDate()))
        .subChallenges(request.getSubChallenges())
        .groupId(request.getGroupId())
        .creatingUserId("5e8ee10b49ff1862a40c8c72")
        .build();
    Challenge addedChallenge = challengeRepo.save(challenge);
    Group group = groupRepo.findById(request.getGroupId()).get();
    group.getChallenges().add(addedChallenge);
    groupRepo.save(group);
    response.setData(addedChallenge);
    return response;
  }

  public Date parseDate(String dateString) {
    ZonedDateTime zonedDate = ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_ZONED_DATE_TIME);
    return Date.from(zonedDate.withZoneSameInstant(ZoneOffset.UTC).toInstant());
  }

  @GetMapping(path = "/challenge", produces = "application/json")
  public Group getChallenge() {
    Group group = groupRepo.findById("5e8ee10b49ff1862a40c8c73").get();
    return group;
  }

  @GetMapping(path = "/challenge2", produces = "application/json")
  public List<Challenge> getChallenge2() {
    Group group = groupRepo.findById("5e8ee10b49ff1862a40c8c73").get();
    return group.getChallenges();
  }

  @GetMapping(path = "/challenge3", produces = "application/json")
  public Challenge getChallenge3() {
    Group group = groupRepo.findById("5e8ee10b49ff1862a40c8c73").get();
    return group.getChallenges().get(0);
  }

}
