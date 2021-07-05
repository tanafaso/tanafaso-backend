package com.azkar.entities.challenges;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;

@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class Challenge {

  @Indexed
  @Id
  private String id;
  @NotNull
  @Indexed(name = "groupId_index")
  private String groupId;
  @NotNull
  private String creatingUserId;
  private long expiryDate;
  @Default
  private List<String> usersFinished = new ArrayList<>();

  @JsonIgnore
  @CreatedDate
  private long createdAt;
  @JsonIgnore
  @LastModifiedDate
  private long modifiedAt;
}
