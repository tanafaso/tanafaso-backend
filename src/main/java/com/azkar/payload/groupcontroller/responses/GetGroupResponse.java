package com.azkar.payload.groupcontroller.responses;

import com.azkar.entities.Group;
import com.azkar.payload.ResponseBase;

public class GetGroupResponse extends ResponseBase<Group> {

  public static final String NOT_MEMBER_IN_GROUP_ERROR = "Not a member in this group";

}
