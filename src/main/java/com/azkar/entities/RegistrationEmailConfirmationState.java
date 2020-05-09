package com.azkar.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "registration_with_email_confirmation_states")
@Builder
@Data
public class RegistrationEmailConfirmationState extends EntityBase {

  @Id
  private String id;
  @NonNull
  @Indexed(name = "email_index", unique = true)
  private String email;
  @NonNull
  private String password;
  @NonNull
  private Integer pin;
  @NonNull
  private String name;

  @JsonIgnore
  @CreatedDate
  private long createdAt;
  @JsonIgnore
  @LastModifiedDate
  private long updatedAt;
}
