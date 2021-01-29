package com.azkar.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "groups")
@Getter
@Builder(toBuilder = true)
public class Group extends EntityBase {

  @Id
  private String id;
  @NonNull
  private String name;
  @NonNull
  private String adminId;
  @NonNull
  private List<String> usersIds;
  @Default
  private List<String> challengesIds = new ArrayList<>();
  @JsonIgnore
  @CreatedDate
  private long createdAt;
  @JsonIgnore
  @LastModifiedDate
  private long modifiedAt;
}
