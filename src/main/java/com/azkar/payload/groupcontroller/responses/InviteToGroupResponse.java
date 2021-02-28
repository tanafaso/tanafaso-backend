package com.azkar.payload.groupcontroller.responses;

import com.azkar.payload.ResponseBase;

public class InviteToGroupResponse extends ResponseBase {

  public static final String GROUP_INVALID_ERROR = "Invalid group ID.";
  public static final String INVITED_USER_INVALID_ERROR = "Invalid invited user ID.";
  public static final String INVITING_USER_IS_NOT_MEMBER_ERROR =
      "Inviting user is not a member of the group.";
  public static final String INVITED_USER_ALREADY_MEMBER_ERROR =
      "Invited user is already a member of the group.";
  public static final String USER_ALREADY_INVITED_ERROR =
      "Inviting user has already invited this user to this group.";

}
