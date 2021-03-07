package com.azkar.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "friendships")
@CompoundIndex(name = "user_user", def = "{'requesterId': 1, 'responderId': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Friendship extends EntityBase {

  @Id
  private String id;
  @NonNull
  @Indexed(name = "user_id_index")
  private String userId;
  @Default
  private List<Friend> friends = new ArrayList<Friend>();
  @JsonIgnore
  @CreatedDate
  private long createdAt;
  @JsonIgnore
  @LastModifiedDate
  private long modifiedAt;

  @Builder
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Friend {

    @NonNull
    private String userId;
    private String groupId;
    @NonNull
    private String username;
    @NonNull
    private String name;
    private boolean isPending;
  }
}
