package com.azkar.entities;

import com.azkar.entities.challenges.AzkarChallenge;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User extends EntityBase {

  // Sabeq is a user that is added by default as a friend for all users so that new users can try
  // the application by sending challenges to sabeq and seeing him on the leaderboard and also so
  // that users can use him to create personal challenges.
  public static final String SABEQ_ID = "60d18088076b0b7d53e5a35a";

  @Indexed
  @Id
  private String id;
  private String email;
  @JsonIgnore
  private String encodedPassword;
  @JsonIgnore
  private String notificationsToken;
  // These Challenge instances are not documents in the challenges collection.
  @Deprecated
  @Indexed(name = "username_index", unique = true)
  @Default
  private List<AzkarChallenge> personalChallenges = new ArrayList<>();
  private String username;
  @NotNull
  private String firstName;
  @NotNull
  private String lastName;
  private UserFacebookData userFacebookData;
  @Default
  private List<UserGroup> userGroups = new ArrayList();
  // Every Challenge instance in this list is a user-customized-copy of a Challenge document in
  // the challenges collection.
  // Use azkarChallenges or meaningChallenges instead.
  @Deprecated
  @Default
  private List<AzkarChallenge> userChallenges = new ArrayList();
  @Default
  private List<AzkarChallenge> azkarChallenges = new ArrayList();
  @Default
  private List<AzkarChallenge> meaningChallenges = new ArrayList();
  @JsonIgnore
  @Default
  private String resetPasswordToken = "";
  @JsonIgnore
  private long resetPasswordTokenExpiryTime;
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
    // Group name can be empty or null in case it is an auto-generated group.
    private String groupName;
    @NotNull
    private String invitingUserId;
    @Default
    private int monthScore = 0;
    @Default
    private int totalScore = 0;
  }

  @Builder(toBuilder = true)
  @Data
  @AllArgsConstructor
  public static class UserFacebookData {

    @Indexed(name = "user_facebook_data_index", unique = true)
    String userId;
    String accessToken;
    String email;
    @NotNull
    String firstName;
    @NotNull
    String lastName;
  }
}
