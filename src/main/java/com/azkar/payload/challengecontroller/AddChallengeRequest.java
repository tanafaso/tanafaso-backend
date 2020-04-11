package com.azkar.payload.challengecontroller;

import com.azkar.entities.Challenge.Subchallenges;
import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddChallengeRequest {
  String groupId;
  String motivation;
  String name;
  String expiryDate;
  List<Subchallenges> subChallenges;
}
