package com.azkar.entities;

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
@Builder
@Data
public class User extends EntityBase {

  @Id
  private String id;
  @Indexed(name = "email_index", unique = true)
  private String email;
  @Indexed(name = "username_index", unique = true)
  private String username;
  private String name;
  @Default
  private List<UserGroup> userGroups = new ArrayList();
  @CreatedDate
  private long createdAt;
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
}
