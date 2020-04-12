package com.azkar.entities;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
@Builder
@Data
public class User {

  @Id
  private String id;
  @Indexed(name = "email_index", unique = true)
  private String email;
  @Indexed(name = "username_index", unique = true)
  private PersonalGroup personalGroup;
  private String username;
  private String name;
}
