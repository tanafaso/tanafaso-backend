package com.azkar.entities;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "groups")
@Getter
@SuperBuilder
@NoArgsConstructor
public class Group extends GroupBase {

  @Id
  private String id;
  @NonNull
  private List<String> usersIds;
  @Default
  private List<String> challenges = new ArrayList<>();

}
