package com.azkar.controllers;

import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.entities.User.UserGroup;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.groupcontroller.requests.AddGroupRequest;
import com.azkar.payload.groupcontroller.responses.AcceptGroupInvitationResponse;
import com.azkar.payload.groupcontroller.responses.AddGroupResponse;
import com.azkar.payload.groupcontroller.responses.GetGroupResponse;
import com.azkar.payload.groupcontroller.responses.GetUserGroupsResponse;
import com.azkar.payload.groupcontroller.responses.InviteToGroupResponse;
import com.azkar.payload.groupcontroller.responses.LeaveGroupResponse;
import com.azkar.payload.groupcontroller.responses.RejectGroupInvitationResponse;
import com.azkar.payload.usercontroller.GetUserResponse;
import com.azkar.repos.GroupRepo;
import com.azkar.repos.UserRepo;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/groups", produces = MediaType.APPLICATION_JSON_VALUE)
public class GroupController extends BaseController {

  @Autowired
  private GroupRepo groupRepo;

  @Autowired
  private UserRepo userRepo;

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  // @TODO(3bza): Validate AddGroupRequest.
  public ResponseEntity<AddGroupResponse> addGroup(@RequestBody AddGroupRequest req) {
    AddGroupResponse response = new AddGroupResponse();
    if (Strings.isNullOrEmpty(req.getName())) {
      response.setError(new Error("Cannot create a group with empty name."));
      return ResponseEntity.badRequest().body(response);
    }
    User currentUser = getCurrentUser(userRepo);
    Group newGroup =
        Group.builder()
            .name(req.getName())
            .adminId(currentUser.getId())
            .isBinary(true)
            .usersIds(new ArrayList<>(Arrays.asList(currentUser.getId())))
            .build();
    newGroup = groupRepo.save(newGroup);

    currentUser.getUserGroups()
        .add(UserGroup.builder().groupId(newGroup.getId()).groupName(newGroup.getName())
            .isPending(false)
            .build());
    userRepo.save(currentUser);

    response.setData(newGroup);
    return ResponseEntity.ok(response);
  }

  @GetMapping(value = "/{groupId}")
  public ResponseEntity<GetGroupResponse> getGroup(@PathVariable String groupId) {
    GetGroupResponse response = new GetGroupResponse();
    User currentUser = getCurrentUser(userRepo);
    if (!currentUser.getUserGroups().stream().anyMatch(
        userGroup ->
            userGroup.getGroupId().equals(groupId)
    )) {
      response.setError(new Error(GetGroupResponse.NOT_MEMBER_IN_GROUP_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    Optional<Group> group = groupRepo.findById(groupId);
    // Check whether the group is deleted.
    if (!group.isPresent()) {
      response.setError(new Error(GetGroupResponse.NOT_MEMBER_IN_GROUP_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    response.setData(group.get());
    return ResponseEntity.ok(response);
  }

  @GetMapping
  public ResponseEntity<GetUserGroupsResponse> getGroups() {
    GetUserGroupsResponse response = new GetUserGroupsResponse();

    User user = userRepo.findById(getCurrentUser().getUserId()).get();
    response.setData(user.getUserGroups());

    return ResponseEntity.ok(response);
  }

  @PutMapping(value = "/{groupId}/invite/{userId}")
  public ResponseEntity<InviteToGroupResponse> invite(
      @PathVariable String groupId,
      @PathVariable(value = "userId") String invitedUserId) {
    InviteToGroupResponse response = new InviteToGroupResponse();

    // Check if the invited user id is valid.
    Optional<User> invitedUser = userRepo.findById(invitedUserId);
    if (!invitedUser.isPresent()) {
      response.setError(new Error(InviteToGroupResponse.INVITED_USER_INVALID_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    // Check if the group id is valid.
    Optional<Group> group = groupRepo.findById(groupId);
    if (!group.isPresent()) {
      response.setError(new Error(InviteToGroupResponse.GROUP_INVALID_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    // Check if the inviting user is a member of the group.
    if (!isMember(userRepo.findById(getCurrentUser().getUserId()).get(), group.get())) {
      response.setError(new Error(InviteToGroupResponse.INVITING_USER_IS_NOT_MEMBER_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    // Check if the invited user is already a member of the group.
    if (isMember(invitedUser.get(), group.get())) {
      response.setError(new Error(InviteToGroupResponse.INVITED_USER_ALREADY_MEMBER_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    // Check if the inviting user has already invited the invited user to this group.
    User invitingUser = userRepo.findById(getCurrentUser().getUserId()).get();
    if (invitedUser.get().getUserGroups().stream().anyMatch(
        userGroup ->
            (userGroup.getGroupId().equals(groupId)
                && userGroup.getInvitingUserId().equals(invitingUser.getId())))) {
      response.setError(new Error(InviteToGroupResponse.USER_ALREADY_INVITED_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    invitedUser.get().getUserGroups().add(
        UserGroup.builder()
            .groupId(groupId)
            .groupName(group.get().getName())
            .invitingUserId(invitingUser.getId())
            .isPending(true)
            .build());
    userRepo.save(invitedUser.get());
    return ResponseEntity.ok(response);
  }

  @PutMapping(value = "/{groupId}/accept")
  public ResponseEntity<AcceptGroupInvitationResponse> accept(@PathVariable String groupId) {
    AcceptGroupInvitationResponse response = new AcceptGroupInvitationResponse();

    User user = userRepo.findById(getCurrentUser().getUserId()).get();
    Optional<Group> group = groupRepo.findById(groupId);

    // Check if the group id is valid.
    if (!group.isPresent()) {
      response.setError(new Error(AcceptGroupInvitationResponse.GROUP_INVALID_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    // Check if the user is already a member of the group.
    if (isMember(user, group.get())) {
      response.setError(new Error(AcceptGroupInvitationResponse.USER_ALREADY_MEMBER_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    // Check if the user is not invited to the group.
    if (!isInvited(user, group.get())) {
      response.setError(new Error(AcceptGroupInvitationResponse.USER_NOT_INVITED_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    // Add the user as a member to this group.
    group.get().getUsersIds().add(user.getId());
    groupRepo.save(group.get());

    List<UserGroup> userGroups = user.getUserGroups();
    // Remove all of the invitations of this group to this user.
    userGroups.removeIf(userGroup -> userGroup.getGroupId().equals(groupId));

    // Add the group to the list of this user groups.
    userGroups.add(UserGroup.builder()
        .groupId(groupId)
        .groupName(group.get().getName())
        .isPending(false)
        .build());
    userRepo.save(user);

    return ResponseEntity.ok(response);
  }

  @PutMapping(value = "/{groupId}/leave")
  public ResponseEntity<LeaveGroupResponse> leave(@PathVariable String groupId) {
    LeaveGroupResponse response = new LeaveGroupResponse();

    User user = userRepo.findById(getCurrentUser().getUserId()).get();
    Optional<Group> group = groupRepo.findById(groupId);

    // Check if the group id is valid.
    if (!group.isPresent()) {
      response.setError(new Error(LeaveGroupResponse.GROUP_INVALID_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    // Check if the user is not already a member of the group.
    if (!isMember(user, group.get())) {
      response.setError(new Error(LeaveGroupResponse.NOT_MEMBER_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    group.get().getUsersIds().removeIf(userId -> userId.equals(user.getId()));
    groupRepo.save(group.get());

    List<UserGroup> userGroups = user.getUserGroups();
    userGroups.removeIf(userGroup -> userGroup.getGroupId().equals(groupId));
    userRepo.save(user);

    return ResponseEntity.ok(response);
  }

  @PutMapping(value = "/{groupId}/reject")
  public ResponseEntity<RejectGroupInvitationResponse> reject(@PathVariable String groupId) {
    RejectGroupInvitationResponse response = new RejectGroupInvitationResponse();

    User user = userRepo.findById(getCurrentUser().getUserId()).get();
    Optional<Group> group = groupRepo.findById(groupId);

    // Check if the group id is valid.
    if (!group.isPresent()) {
      response.setError(new Error(RejectGroupInvitationResponse.GROUP_INVALID_ERROR));
      return ResponseEntity.badRequest().body(response);
    }

    // Check if the user is already a member of the group.
    if (isMember(user, group.get())) {
      response.setError(new Error(RejectGroupInvitationResponse.USER_ALREADY_MEMBER_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    // Check if the user is not invited to the group.
    if (!isInvited(user, group.get())) {
      response.setError(new Error(RejectGroupInvitationResponse.USER_NOT_INVITED_ERROR));
      return ResponseEntity.unprocessableEntity().body(response);
    }

    List<UserGroup> userGroups = user.getUserGroups();
    // Remove all of the invitations of this group to this user.
    userGroups.removeIf(userGroup -> userGroup.getGroupId().equals(groupId));
    userRepo.save(user);

    return ResponseEntity.ok(response);
  }

  private boolean isMember(User user, Group group) {
    return group.getUsersIds().stream().anyMatch(userId -> userId.equals(user.getId()));
  }

  private boolean isInvited(User user, Group group) {
    return user.getUserGroups().stream().anyMatch(
        userGroup -> (userGroup.getGroupId().equals(group.getId()) && userGroup.isPending()));
  }

}
