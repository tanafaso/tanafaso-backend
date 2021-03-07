package com.azkar.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
@Builder(toBuilder = true)
@Data
public class User extends EntityBase {

  @Id
  private String id;
  private String email;
  @JsonIgnore
  private String encodedPassword;
  // These Challenge instances are not documents in the challenges collection.
  @Indexed(name = "username_index", unique = true)
  @Default
  private List<Challenge> personalChallenges = new ArrayList<>();
  private String username;
  @NonNull
  private String name;
  private UserFacebookData userFacebookData;
  @Default
  private List<UserGroup> userGroups = new ArrayList();
  // Every Challenge instance in this list is a user-customized-copy of a Challenge document in
  // the challenges collection.
  @Default
  private List<Challenge> userChallenges = new ArrayList();
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
    @NotNull
    private String groupName;
    // Can be null in case of a non-pending group.
    private String invitingUserId;
    private boolean isPending;
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
    @NonNull
    String name;
  }
}
