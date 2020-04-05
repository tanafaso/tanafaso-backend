package com.azkar.payload.groupcontroller;

import com.azkar.entities.Group;
import com.azkar.payload.ResponseBase;

public class AddGroupResponse extends ResponseBase<Group> {

  public AddGroupResponse(Group group) {
    setData(group);
  }

  public AddGroupResponse(Error error) {
    setError(error);
  }
}
