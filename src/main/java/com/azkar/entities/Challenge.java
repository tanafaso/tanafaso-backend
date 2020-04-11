package com.azkar.entities;

import com.mongodb.lang.NonNull;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Builder
@Getter
@Document(collection = "challenges")
public class Challenge {
  @Id
  private String id;
  @NonNull
  private String groupId; // !!!!
  private String creatingUserId; // !!!!
  private String motivation;
  private String name;
  private Date expiryDate;
  private boolean isOngoing;
  private List<String> usersAccepted; // !!!!
  private List<String> usersFinished; // !!!!
  private List<Subchallenges> subChallenges;
  @CreatedDate
  private Date createdAt;
  @LastModifiedDate
  private Date modifiedAt;

  @Getter
  @Setter
  public static class Subchallenges {
    private String zekr;
    private int originalRepititons;
    private int leftRepititons;

    public Subchallenges(String zerk, int repititons) {
      this.zekr = zerk;
      this.originalRepititons = this.leftRepititons = repititons;
    }
  }
}
