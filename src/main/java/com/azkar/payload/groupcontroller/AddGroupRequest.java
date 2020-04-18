package com.azkar.payload.groupcontroller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class AddGroupRequest {

  private String name;
  private boolean isBinary;
}
