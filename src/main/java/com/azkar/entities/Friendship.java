package com.azkar.entities;

import java.util.Date;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "friendships")
@CompoundIndex(name = "user_user", def = "{'requesterId': 1, 'responderId': 1}", unique = true)
@Data
@Builder
public class Friendship {

  @Id
  private String id;
  private String requesterId;
  private String requesterUsername;
  private String responderId;
  private String responderUsername;
  private boolean isPending;
  @CreatedDate
  private Date createdAt;
  @LastModifiedDate
  private Date modifiedAt;
}
