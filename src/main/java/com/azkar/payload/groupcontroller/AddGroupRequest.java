package com.azkar.payload.groupcontroller;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AddGroupRequest {

  String name;
  boolean isBinary;
}
