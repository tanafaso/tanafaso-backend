package com.azkar.entities;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "groups")
@Builder
@Getter
public class Group {

  @NonNull
  private final String name;
  @NonNull
  private final GroupCardinality cardinality;
  @NonNull
  private final String adminId;
  @Id
  private String id;
  @NonNull
  private List<String> usersIds;
  @DBRef
  @Default
  private List<Challenge> challenges = new ArrayList<>();
  @CreatedDate
  private Date createdAt;
  @LastModifiedDate
  private Date modifiedAt;

  public enum GroupCardinality {
    SINGLE,
    DOUBLE,
    MULTI
  }
}
