package com.azkar.payload.challengecontroller.responses;

import com.azkar.payload.ResponseBase;
import com.azkar.payload.challengecontroller.responses.utils.UserReturnedChallenge;
import java.util.List;

public class GetChallengesResponse extends ResponseBase<List<UserReturnedChallenge>> {

  public static final String GROUP_NOT_FOUND_ERROR = "Group not found.";

}
