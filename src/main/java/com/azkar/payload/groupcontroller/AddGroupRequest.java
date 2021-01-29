package com.azkar.payload.groupcontroller;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
@Getter
public class AddGroupRequest {

  @NonNull
  private String name;
  private boolean isBinary;
}
