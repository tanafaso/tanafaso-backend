package com.azkar.entities;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "groups")
@Getter
@Builder
@NoArgsConstructor
public class Group {
  @Id
  private String id;
  @NonNull
  private boolean isBinary;
  @NonNull
  private String name;
  @NonNull
  private String adminId;
  @NonNull
  private List<String> usersIds;
  @Default
  private List<String> challenges = new ArrayList<>();
  @CreatedDate
  private long createdAt;
  @LastModifiedDate
  private long modifiedAt;
}
