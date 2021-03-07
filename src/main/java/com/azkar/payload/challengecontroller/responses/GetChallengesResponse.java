package com.azkar.payload.challengecontroller.responses;

import com.azkar.entities.Challenge;
import com.azkar.payload.ResponseBase;
import java.util.List;

public class GetChallengesResponse extends ResponseBase<List<Challenge>> {

  public static final String GROUP_NOT_FOUND_ERROR = "Group not found.";
  public static final String NON_GROUP_MEMBER_ERROR = "The user is not a member of this group.";

}
