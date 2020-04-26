package com.azkar.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mongodb.lang.NonNull;
import java.util.ArrayList;
import java.util.List;
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

  @Id
  private String id;
  @NonNull
  @Indexed(name = "groupId_index")
  private String groupId;
  @NonNull
  private String creatingUserId;
  @NonNull
  private String motivation;
  @NonNull
  private String name;
  private long expiryDate;
  private boolean isOngoing;
  @NonNull
  private List<String> usersAccepted;
  @Default
  private List<String> usersFinished = new ArrayList<>();
  @NonNull
  private List<SubChallenges> subChallenges;
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
  @Builder
  public static class SubChallenges {

    @NonNull
    private String zekr;
    private int originalRepetitions;
    private int leftRepetitions;
  }
}
