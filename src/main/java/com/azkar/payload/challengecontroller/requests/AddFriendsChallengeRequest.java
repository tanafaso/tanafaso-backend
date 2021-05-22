package com.azkar.payload.challengecontroller.requests;

import com.azkar.entities.Challenge;
import com.azkar.payload.ResponseBase.Status;
import com.azkar.payload.exceptions.BadRequestException;
import java.util.HashSet;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class AddFriendsChallengeRequest extends AddChallengeRequest {

  private List<String> friendsIds;

  @Builder(builderMethodName = "AddFriendsChallengeRequestBuilder")
  public AddFriendsChallengeRequest(List<String> friendsIds, Challenge challenge) {
    this.challenge = challenge;
    this.friendsIds = friendsIds;
  }

  @Override
  public void validate() throws BadRequestException {
    // NOTE: The challenge group ID can be null in this case as the group will be auto-generated.
    checkNotNull(challenge.getName(), challenge.getSubChallenges());

    validateFriendIds();
    validateExpiryDate();
    validateSubChallenges();
  }

  private void validateFriendIds() {
    if (new HashSet<>(friendsIds).size() != friendsIds.size()) {
      throw new BadRequestException(new Status(Status.DUPLICATE_FRIEND_IDS_PROVIDED_ERROR));
    }
  }
}
