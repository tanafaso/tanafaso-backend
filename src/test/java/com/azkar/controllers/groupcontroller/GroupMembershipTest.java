package com.azkar.controllers.groupcontroller;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.controllers.ControllerTestBase;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.entities.User.UserGroup;
import com.azkar.factories.GroupFactory;
import com.azkar.factories.UserFactory;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.groupcontroller.AcceptGroupInvitationResponse;
import com.azkar.payload.groupcontroller.GetUserGroupsResponse;
import com.azkar.payload.groupcontroller.InviteToGroupResponse;
import com.azkar.payload.groupcontroller.LeaveGroupResponse;
import com.azkar.payload.groupcontroller.RejectGroupInvitationResponse;
import com.azkar.repos.GroupRepo;
import com.azkar.repos.UserRepo;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

public class GroupMembershipTest extends ControllerTestBase {

  @Autowired
  GroupRepo groupRepo;

  @Autowired
  UserRepo userRepo;

  private User user1 = UserFactory.getNewUser();
  private User user2 = UserFactory.getNewUser();
  private User user3 = UserFactory.getNewUser();
  private User user4 = UserFactory.getNewUser();
  // A user who have never been authenticated and never added to the database.
  private User invalidUser = UserFactory.getNewUser();

  @Before
  public void before() {
    addNewUser(user1);
    addNewUser(user2);
    addNewUser(user3);
    addNewUser(user4);
  }

  @Test
  public void invite_normalScenario_shouldSucceed() throws Exception {
    Group user1Group = GroupFactory.getNewGroup(user1.getId());
    groupRepo.save(user1Group);

    InviteToGroupResponse expectedResponse = new InviteToGroupResponse();
    inviteUserToGroup(user1, user2, user1Group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(mapToJson(expectedResponse)));

    User user2InRepo = userRepo.findById(user2.getId()).get();
    List<UserGroup> user2Groups = user2InRepo.getUserGroups();
    assertThat(user2Groups.size(), is(1));

    UserGroup user2Group = user2Groups.get(0);
    assertThat(user2Group.getGroupId(), is(user1Group.getId()));
    assertThat(user2Group.getInvitingUserId(), is(user1.getId()));
    assertThat(user2Group.isPending(), is(true));
  }

  @Test
  public void invite_invalidOtherUser_shouldNotSucceed() throws Exception {
    Group user1Group = GroupFactory.getNewGroup(user1.getId());
    groupRepo.save(user1Group);

    InviteToGroupResponse expectedResponse = new InviteToGroupResponse();
    expectedResponse.setError(new Error(InviteToGroupResponse.INVITED_USER_INVALID_ERROR));
    inviteUserToGroup(user1, invalidUser, user1Group.getId())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(mapToJson(expectedResponse)));
  }

  @Test
  public void invite_invalidGroup_shouldNotSucceed() throws Exception {
    Group unSavedGroup = GroupFactory.getNewGroup(user1.getId());

    InviteToGroupResponse expectedResponse = new InviteToGroupResponse();
    expectedResponse.setError(new Error(InviteToGroupResponse.GROUP_INVALID_ERROR));
    inviteUserToGroup(user1, user2, unSavedGroup.getId())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(mapToJson(expectedResponse)));

    User user2InRepo = userRepo.findById(user2.getId()).get();
    List<UserGroup> user2Groups = user2InRepo.getUserGroups();
    assertThat(user2Groups.size(), is(0));
  }

  @Test
  public void invite_otherUserAlreadyMember_shouldNotSucceed() throws Exception {
    Group user1Group = GroupFactory.getNewGroup(user1.getId());
    groupRepo.save(user1Group);

    // Add user2 to group.
    addUserToGroup(user2, user1, user1Group.getId());

    InviteToGroupResponse expectedResponse = new InviteToGroupResponse();
    expectedResponse.setError(new Error(InviteToGroupResponse.INVITED_USER_ALREADY_MEMBER_ERROR));
    inviteUserToGroup(user1, user2, user1Group.getId())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(mapToJson(expectedResponse)));

  }

  @Test
  public void invite_invitingUserIsNotMember_shouldNotSucceed() throws Exception {
    Group user1Group = GroupFactory.getNewGroup(user1.getId());
    groupRepo.save(user1Group);

    InviteToGroupResponse expectedResponse = new InviteToGroupResponse();
    expectedResponse.setError(new Error(InviteToGroupResponse.INVITING_USER_IS_NOT_MEMBER_ERROR));
    inviteUserToGroup(user2, user3, user1Group.getId())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(mapToJson(expectedResponse)));

    User user3InRepo = userRepo.findById(user3.getId()).get();
    List<UserGroup> user3Groups = user3InRepo.getUserGroups();
    assertThat(user3Groups.size(), is(0));
  }

  @Test
  public void invite_invitingUserAlreadyInvitedOtherUser_shouldNotSucceed() throws Exception {
    Group user1Group = GroupFactory.getNewGroup(user1.getId());
    groupRepo.save(user1Group);

    InviteToGroupResponse expectedResponse = new InviteToGroupResponse();
    inviteUserToGroup(user1, user2, user1Group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(mapToJson(expectedResponse)));

    expectedResponse.setError(new Error(InviteToGroupResponse.USER_ALREADY_INVITED_ERROR));
    inviteUserToGroup(user1, user2, user1Group.getId())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(mapToJson(expectedResponse)));

    User user2InRepo = userRepo.findById(user2.getId()).get();
    List<UserGroup> user2Groups = user2InRepo.getUserGroups();
    assertThat(user2Groups.size(), is(1));

    UserGroup user2Group = user2Groups.get(0);
    assertThat(user2Group.getGroupId(), is(user1Group.getId()));
    assertThat(user2Group.getInvitingUserId(), is(user1.getId()));
    assertThat(user2Group.isPending(), is(true));
  }

  @Test
  public void invite_invitedUserAlreadyInvitedByAnotherUser_shouldSucceed() throws Exception {
    Group user1Group = GroupFactory.getNewGroup(user1.getId());
    // Make user2 a member of the group.
    user1Group.getUsersIds().add(user2.getId());
    groupRepo.save(user1Group);

    InviteToGroupResponse expectedResponse = new InviteToGroupResponse();
    inviteUserToGroup(user1, user3, user1Group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(mapToJson(expectedResponse)));

    inviteUserToGroup(user2, user3, user1Group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(mapToJson(expectedResponse)));

    User user3InRepo = userRepo.findById(user3.getId()).get();
    List<UserGroup> user3Groups = user3InRepo.getUserGroups();
    assertThat(user3Groups.size(), is(2));

    UserGroup user3GroupInvitation1 = user3Groups.get(0);
    assertThat(user3GroupInvitation1.getGroupId(), is(user1Group.getId()));
    assertThat(user3GroupInvitation1.getInvitingUserId(), is(user1.getId()));
    assertThat(user3GroupInvitation1.isPending(), is(true));

    UserGroup user3GroupInvitation2 = user3Groups.get(1);
    assertThat(user3GroupInvitation2.getGroupId(), is(user1Group.getId()));
    assertThat(user3GroupInvitation2.getInvitingUserId(), is(user2.getId()));
    assertThat(user3GroupInvitation2.isPending(), is(true));

  }

  @Test
  public void accept_normalScenario_shouldSucceed() throws Exception {
    Group user1Group = GroupFactory.getNewGroup(user1.getId());
    groupRepo.save(user1Group);

    // user1 invites user2.
    InviteToGroupResponse expectedInviteToGroupResponse = new InviteToGroupResponse();
    inviteUserToGroup(user1, user2, user1Group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(mapToJson(expectedInviteToGroupResponse)));

    // user2 accepts invitation.
    AcceptGroupInvitationResponse expectedAcceptGroupInvitationResponse =
        new AcceptGroupInvitationResponse();
    acceptInvitationToGroup(user2, user1Group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedAcceptGroupInvitationResponse)));

    User user2InRepo = userRepo.findById(user2.getId()).get();
    List<UserGroup> user2Groups = user2InRepo.getUserGroups();
    assertThat(user2Groups.size(), is(1));

    UserGroup user2Group = user2Groups.get(0);
    assertThat(user2Group.getGroupId(), is(user1Group.getId()));
    assertThat(user2Group.isPending(), is(false));
  }

  @Test
  public void accept_invalidGroup_shouldNotSucceed() throws Exception {
    Group unSavedGroup = GroupFactory.getNewGroup(user1.getId());

    AcceptGroupInvitationResponse expectedResponse = new AcceptGroupInvitationResponse();
    expectedResponse.setError(new Error(InviteToGroupResponse.GROUP_INVALID_ERROR));
    inviteUserToGroup(user1, user2, unSavedGroup.getId())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(mapToJson(expectedResponse)));

    User user2InRepo = userRepo.findById(user2.getId()).get();
    List<UserGroup> user2Groups = user2InRepo.getUserGroups();
    assertThat(user2Groups.size(), is(0));
  }

  @Test
  public void accept_notInvited_shouldNotSucceed() throws Exception {
    Group user1Group = GroupFactory.getNewGroup(user1.getId());
    groupRepo.save(user1Group);

    AcceptGroupInvitationResponse expectedResponse =
        new AcceptGroupInvitationResponse();
    expectedResponse.setError(new Error(AcceptGroupInvitationResponse.USER_NOT_INVITED_ERROR));
    acceptInvitationToGroup(user2, user1Group.getId())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));

    User user2InRepo = userRepo.findById(user2.getId()).get();
    List<UserGroup> user2Groups = user2InRepo.getUserGroups();
    assertThat(user2Groups.size(), is(0));
  }

  @Test
  public void accept_AlreadyMember_shouldNotSucceed() throws Exception {
    Group user1Group = GroupFactory.getNewGroup(user1.getId());
    groupRepo.save(user1Group);

    // user1 invites user2.
    InviteToGroupResponse expectedInviteToGroupResponse = new InviteToGroupResponse();
    inviteUserToGroup(user1, user2, user1Group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(mapToJson(expectedInviteToGroupResponse)));

    // user2 accepts group invitation.
    AcceptGroupInvitationResponse expectedAcceptGroupInvitationResponse =
        new AcceptGroupInvitationResponse();
    acceptInvitationToGroup(user2, user1Group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedAcceptGroupInvitationResponse)));

    // user2 accepts group invitation again.
    expectedAcceptGroupInvitationResponse
        .setError(new Error(AcceptGroupInvitationResponse.USER_ALREADY_MEMBER_ERROR));
    acceptInvitationToGroup(user2, user1Group.getId())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedAcceptGroupInvitationResponse)));

    User user2InRepo = userRepo.findById(user2.getId()).get();
    List<UserGroup> user2Groups = user2InRepo.getUserGroups();
    assertThat(user2Groups.size(), is(1));

    UserGroup user2Group = user2Groups.get(0);
    assertThat(user2Group.getGroupId(), is(user1Group.getId()));
    assertThat(user2Group.isPending(), is(false));
  }

  @Test
  public void reject_normalScenario_shouldSucceed() throws Exception {
    Group user1Group = GroupFactory.getNewGroup(user1.getId());
    groupRepo.save(user1Group);

    // user1 invites user2.
    InviteToGroupResponse expectedInviteToGroupResponse = new InviteToGroupResponse();
    inviteUserToGroup(user1, user2, user1Group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(mapToJson(expectedInviteToGroupResponse)));

    // user2 rejects invitation.
    RejectGroupInvitationResponse expectedAcceptGroupInvitationResponse =
        new RejectGroupInvitationResponse();
    rejectInvitationToGroup(user2, user1Group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedAcceptGroupInvitationResponse)));

    User user2InRepo = userRepo.findById(user2.getId()).get();
    List<UserGroup> user2Groups = user2InRepo.getUserGroups();
    assertThat(user2Groups.size(), is(0));

    Group group = groupRepo.findById(user1Group.getId()).get();
    assertThat(group.getUsersIds().size(), is(1));
  }

  @Test
  public void reject_notInvited_shouldNotSucceed() throws Exception {
    Group user1Group = GroupFactory.getNewGroup(user1.getId());
    groupRepo.save(user1Group);

    RejectGroupInvitationResponse expectedResponse =
        new RejectGroupInvitationResponse();
    expectedResponse.setError(new Error(RejectGroupInvitationResponse.USER_NOT_INVITED_ERROR));
    rejectInvitationToGroup(user2, user1Group.getId())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));

    User user2InRepo = userRepo.findById(user2.getId()).get();
    List<UserGroup> user2Groups = user2InRepo.getUserGroups();
    assertThat(user2Groups.size(), is(0));
  }

  @Test
  public void reject_alreadyMember_shouldNotSucceed() throws Exception {
    Group user1Group = GroupFactory.getNewGroup(user1.getId());
    groupRepo.save(user1Group);

    // Add user2 to the group.
    addUserToGroup(user2, user1, user1Group.getId());

    // user2 accepts group invitation again.
    RejectGroupInvitationResponse expectedRejectGroupInvitationResponse =
        new RejectGroupInvitationResponse();
    expectedRejectGroupInvitationResponse
        .setError(new Error(AcceptGroupInvitationResponse.USER_ALREADY_MEMBER_ERROR));
    rejectInvitationToGroup(user2, user1Group.getId())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedRejectGroupInvitationResponse)));

    User user2InRepo = userRepo.findById(user2.getId()).get();
    List<UserGroup> user2Groups = user2InRepo.getUserGroups();
    assertThat(user2Groups.size(), is(1));

    UserGroup user2Group = user2Groups.get(0);
    assertThat(user2Group.getGroupId(), is(user1Group.getId()));
    assertThat(user2Group.isPending(), is(false));
  }

  @Test
  public void leave_normalScenario_shouldSucceed() throws Exception {
    Group user1Group = GroupFactory.getNewGroup(user1.getId());
    groupRepo.save(user1Group);

    // Add user2 to the group.
    addUserToGroup(user2, user1, user1Group.getId());

    LeaveGroupResponse expectedResponse = new LeaveGroupResponse();
    leaveGroup(user2, user1Group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));

    Group group = groupRepo.findById(user1Group.getId()).get();
    assertThat(group.getUsersIds().size(), is(1));
    assertThat(group.getUsersIds().get(0), is(user1.getId()));

    User user2InRepo = userRepo.findById(user2.getId()).get();
    assertThat(user2InRepo.getUserGroups().size(), is(0));
  }

  @Test
  public void leave_invalidGroup_shouldNoSucceed() throws Exception {
    Group unSavedGroup = GroupFactory.getNewGroup(user1.getId());

    LeaveGroupResponse expectedResponse = new LeaveGroupResponse();
    expectedResponse.setError(new Error(LeaveGroupResponse.GROUP_INVALID_ERROR));
    leaveGroup(user2, unSavedGroup.getId())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
  }

  @Test
  public void leave_notMember_shouldNoSucceed() throws Exception {
    Group user1Group = GroupFactory.getNewGroup(user1.getId());
    groupRepo.save(user1Group);

    LeaveGroupResponse expectedResponse = new LeaveGroupResponse();
    expectedResponse.setError(new Error(LeaveGroupResponse.NOT_MEMBER_ERROR));
    leaveGroup(user2, user1Group.getId())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
  }

  @Test
  public void getGroups_normalScenario_shouldSucceed() throws Exception {
    Group group1 = GroupFactory.getNewGroup(user1.getId());
    Group group2 = GroupFactory.getNewGroup(user1.getId());
    Group group3 = GroupFactory.getNewGroup(user2.getId());
    groupRepo.save(group1);
    groupRepo.save(group2);
    groupRepo.save(group3);

    // Add user3 to group1.
    addUserToGroup(user3, user1, group1.getId());

    // Invite user3 to group2.
    inviteUserToGroup(user1, user3, group2.getId());

    // Add user3 to group3.
    addUserToGroup(user3, user2, group3.getId());

    GetUserGroupsResponse expectedResponse = new GetUserGroupsResponse();
    List<UserGroup> expectedUserGroups = new ArrayList();
    expectedUserGroups.add(UserGroup.builder()
        .groupId(group1.getId())
        .isPending(false)
        .build());

    expectedUserGroups.add(UserGroup.builder()
        .groupId(group2.getId())
        .invitingUserId(user1.getId())
        .isPending(true)
        .build());

    expectedUserGroups.add(UserGroup.builder()
        .groupId(group3.getId())
        .isPending(false)
        .build());

    expectedResponse.setData(expectedUserGroups);
    authenticate(user3);
    MvcResult result = performGetRequest("/groups/")
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)))
        .andReturn();

    assertThat(result.getResponse().getContentAsString(), is(mapToJson(expectedResponse)));
  }

  private ResultActions inviteUserToGroup(User invitingUser, User invitedUser, String groupId)
      throws Exception {
    authenticate(invitingUser);
    return performPutRequest(String.format("/groups/%s/invite/%s", groupId, invitedUser.getId()),
        /*body=*/ null);
  }

  private ResultActions acceptInvitationToGroup(User user, String groupId)
      throws Exception {
    authenticate(user);
    return performPutRequest(String.format("/groups/%s/accept/", groupId), /*body=*/ null);
  }

  private ResultActions rejectInvitationToGroup(User user, String groupId)
      throws Exception {
    authenticate(user);
    return performPutRequest(String.format("/groups/%s/reject/", groupId), /*body=*/ null);
  }

  private ResultActions leaveGroup(User user, String groupId) throws Exception {
    authenticate(user);
    return performPutRequest(String.format("/groups/%s/leave/", groupId), /*body=*/ null);
  }

  private void addUserToGroup(User user, User groupAdmin, String groupId)
      throws Exception {
    inviteUserToGroup(groupAdmin, user, groupId);
    acceptInvitationToGroup(user, groupId);
  }
}
