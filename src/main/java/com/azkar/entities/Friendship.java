package com.azkar.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "friendships")
@CompoundIndex(def = "{userId1: 1, userId2: 1}")
public class Friendship {

  @Id
  private String id;
  private String userId1;
  private String userId2;
  private boolean isPending;

  public String getUserId1() {
    return userId1;
  }

  public void setUserId1(String userId1) {
    this.userId1 = userId1;
  }

  public String getUserId2() {
    return userId2;
  }

  public void setUserId2(String userId2) {
    this.userId2 = userId2;
  }

  public boolean isPending() {
    return isPending;
  }

  public void setPending(boolean pending) {
    isPending = pending;
  }
}
