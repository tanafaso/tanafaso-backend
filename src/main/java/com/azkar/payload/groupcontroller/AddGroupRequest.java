package com.azkar.payload.groupcontroller;

import com.azkar.entities.Group.GroupCardinality;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AddGroupRequest {

  String name;
  GroupCardinality cardinality;
}
