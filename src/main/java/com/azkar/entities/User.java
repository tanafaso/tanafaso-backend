package com.azkar.entities;

import com.azkar.entities.Challenge.SubChallenges;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
@Builder
@Data
public class User extends EntityBase {

  @Id
  private String id;
  @Indexed(name = "email_index", unique = true)
  private String email;
  @Indexed(name = "username_index", unique = true)
  @Default
  private List<Challenge> personalChallenges = new ArrayList<>();
  private String username;
  private String name;
  private UserFacebookData userFacebookData;
  @Default
  private List<UserGroup> userGroups = new ArrayList();
  @Default
  private List<UserChallengeStatus> userChallengeStatuses = new ArrayList();
  @JsonIgnore
  @CreatedDate
  private long createdAt;
  @JsonIgnore
  @LastModifiedDate
  private long updatedAt;

  @Builder
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserGroup {

    @NotNull
    private String groupId;
    // Can be null in case of a non-pending group.
    private String invitingUserId;
    private boolean isPending;
    @Default
    private int monthScore = 0;
    @Default
    private int totalScore = 0;
  }

  @Builder(toBuilder = true)
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Setter
  public static class UserChallengeStatus {

    @NonNull
    String challengeId;
    boolean isAccepted;
    boolean isOngoing;
    @NonNull
    List<SubChallenges> subChallenges;
  }

  @Builder
  @Data
  @AllArgsConstructor
  public static class UserFacebookData {

    @Indexed(name = "user_facebook_data_index", unique = true)
    String userId;
    String accessToken;
    String email;
    String name;
  }
}
