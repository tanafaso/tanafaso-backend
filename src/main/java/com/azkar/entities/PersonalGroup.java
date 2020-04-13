package com.azkar.entities;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@NoArgsConstructor
public class PersonalGroup extends GroupBase {

  private GroupCardinality cardinality;
  @Default
  private List<Challenge> challenges = new ArrayList<>();

}
