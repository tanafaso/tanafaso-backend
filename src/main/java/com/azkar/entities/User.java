package com.azkar.entities;

import com.azkar.entities.Challenge.SubChallenges;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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
@Builder(toBuilder = true)
@Data
public class User extends EntityBase {

  @Id
  private String id;
  private String email;
  @JsonIgnore
  private String encodedPassword;
  @Indexed(name = "username_index", unique = true)
  @Default
  private List<Challenge> personalChallenges = new ArrayList<>();
  private String username;
  @NonNull
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
  @JsonIgnoreProperties(value = {"ongoing"})
  public static class UserChallengeStatus {

    @JsonIgnore
    String challengeId;
    boolean isAccepted;
    // This field is ignored using @JsonIgnoreProperties.
    // Using @JsonIgnore does not work with boolean variables named isSomething.
    boolean isOngoing;
    @JsonIgnore
    String groupId;
    @NonNull
    List<UserSubChallenge> subChallenges;
  }

  public static class UserSubChallenge {

    private SubChallenges subChallenge;
    @Getter
    @Setter
    private int leftRepetitions;

    public UserSubChallenge() {
      this.subChallenge = new SubChallenges();
    }

    private UserSubChallenge(SubChallenges subChallenge, int leftRepetitions) {
      this.subChallenge = subChallenge;
      this.leftRepetitions = leftRepetitions;
    }

    public static UserSubChallenge getInstance(SubChallenges subChallenge) {
      return new UserSubChallenge(subChallenge, subChallenge.getOriginalRepetitions());
    }

    public static List<UserSubChallenge> fromSubChallengesCollection(
        List<SubChallenges> subChallenges) {
      return subChallenges.stream().map(UserSubChallenge::getInstance).collect(Collectors.toList());
    }

    public String getZekrId() {
      return subChallenge.getZekrId();
    }

    public void setZekrId(String zekrId) {
      subChallenge.setZekrId(zekrId);
    }

    public String getZekr() {
      return subChallenge.getZekr();
    }

    public void setZekr(String zekr) {
      subChallenge.setZekr(zekr);
    }

    public int getOriginalRepetitions() {
      return subChallenge.getOriginalRepetitions();
    }

    public void setOriginalRepetitions(int originalRepetitions) {
      subChallenge.setOriginalRepetitions(originalRepetitions);
    }
  }

  @Builder
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
