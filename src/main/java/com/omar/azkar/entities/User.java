package com.omar.azkar.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class User {
  @Id
  @GeneratedValue
  private Integer id;
  private String name;

  public User() {
  }

  public Integer getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }
}
