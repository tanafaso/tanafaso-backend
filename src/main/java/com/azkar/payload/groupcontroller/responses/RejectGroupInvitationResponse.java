package com.azkar.payload.groupcontroller.responses;

import com.azkar.payload.ResponseBase;

public class RejectGroupInvitationResponse extends ResponseBase {

  public static final String GROUP_INVALID_ERROR = "Invalid group ID.";
  public static final String USER_ALREADY_MEMBER_ERROR =
      "The user is already a member of this group.";
  public static final String USER_NOT_INVITED_ERROR =
      "The user is not invited to this group.";
}
