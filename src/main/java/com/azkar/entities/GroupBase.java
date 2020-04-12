package com.azkar.entities;

import java.util.Date;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@Getter
@NoArgsConstructor
@SuperBuilder
public abstract class GroupBase {

  @NonNull
  private Group.GroupCardinality cardinality;
  @NonNull
  private String name;
  @NonNull
  private String adminId;
  @CreatedDate
  private Date createdAt;
  @LastModifiedDate
  private Date modifiedAt;
}
