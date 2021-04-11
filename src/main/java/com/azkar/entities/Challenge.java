package com.azkar.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Builder(toBuilder = true)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Document(collection = "challenges")
public class Challenge extends EntityBase {

  /**
   * Personal challenges are represented the same way as normal challenges. One difference is that
   * personal challenges are not saved in a particular group. The solution here is to not get rid of
   * the NonNull annotation over the groupId to make sure that we can get the group for every
   * in-group-challenge. To leave the NonNull annotation we have to provide group IDs also for
   * personal challenges.
   */
  public static final String PERSONAL_CHALLENGES_NON_EXISTING_GROUP_ID
      = "non-existing-personal-challenge-group-id";

  @Id
  private String id;
  @NotNull
  @Indexed(name = "groupId_index")
  private String groupId;
  @NotNull
  private String creatingUserId;
  private String motivation;
  @NotNull
  private String name;
  private long expiryDate;
  @Default
  private List<String> usersFinished = new ArrayList<>();
  @NotNull
  private List<SubChallenge> subChallenges;
  @JsonIgnore
  @CreatedDate
  private long createdAt;
  @JsonIgnore
  @LastModifiedDate
  private long modifiedAt;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder(toBuilder = true)
  public static class SubChallenge {

    private Zekr zekr;
    // Note: This field may have two meanings depending on the context of this SubChallenge. If
    // it is part of a generic Challenge that is saved in the ChallengeRepo then this field means
    // the number of the original repetitions entered on creation. If this is part of the user
    // copy of challenges then it means how many repetitions are left for this zekr for this user.
    private int repetitions;

  }
}
