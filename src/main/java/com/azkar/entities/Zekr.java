package com.azkar.entities;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class Zekr {

  @NotNull
  Integer id;
  @NotNull
  private String zekr;
}
