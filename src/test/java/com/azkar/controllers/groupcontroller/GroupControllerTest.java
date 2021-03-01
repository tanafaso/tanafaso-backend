package com.azkar.controllers.groupcontroller;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.TestBase;
import com.azkar.controllers.utils.JsonHandler;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.entities.User.UserGroup;
import com.azkar.factories.entities.GroupFactory;
import com.azkar.factories.entities.UserFactory;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.groupcontroller.requests.AddGroupRequest;
import com.azkar.payload.groupcontroller.responses.AcceptGroupInvitationResponse;
import com.azkar.payload.groupcontroller.responses.AddGroupResponse;
import com.azkar.payload.groupcontroller.responses.GetUserGroupsResponse;
import com.azkar.payload.groupcontroller.responses.InviteToGroupResponse;
import com.azkar.payload.groupcontroller.responses.LeaveGroupResponse;
import com.azkar.payload.groupcontroller.responses.RejectGroupInvitationResponse;
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

public class GroupControllerTest extends TestBase {

  @Autowired
  GroupRepo groupRepo;

  @Autowired
  UserRepo userRepo;

  private User user1 = UserFactory.getNewUser();
  private User user2 = UserFactory.getNewUser();
  private User user3 = UserFactory.getNewUser();
  private User user4 = UserFactory.getNewUser();
  private Group user1Group = GroupFactory.getNewGroup(user1.getId());
  // A user who have never been authenticated and never added to the database.
  private User invalidUser = UserFactory.getNewUser();

  @Before
  public void before() {
    addNewUser(user1);
    addNewUser(user2);
    addNewUser(user3);
    addNewUser(user4);
    groupRepo.save(user1Group);
  }

  @Test
  public void addGroup_normalScenario_shouldSucceed() throws Exception {
    final String TEST_GROUP_NAME = "example_name";
    AddGroupRequest addGroupRequest = AddGroupRequest.builder().name(TEST_GROUP_NAME).build();
    long groupRepoCountBeforeOperation = groupRepo.count();

    MvcResult result =
        azkarApi.addGroup(user1, addGroupRequest).andExpect(status().isOk()).andReturn();

    AddGroupResponse response = JsonHandler.fromJson(result.getResponse().getContentAsString(),
        AddGroupResponse.class);
    Group returnedGroup = response.getData();
    assertThat(returnedGroup.getName(), is(TEST_GROUP_NAME));
    assertThat(groupRepo.count(), is(groupRepoCountBeforeOperation + 1));
    assertThat(groupRepo.findById(returnedGroup.getId()).isPresent(), is(true));
  }

  @Test
  public void invite_normalScenario_shouldSucceed() throws Exception {
    InviteToGroupResponse expectedResponse = new InviteToGroupResponse();
    inviteUserToGroup(user1, user2, user1Group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

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
    InviteToGroupResponse expectedResponse = new InviteToGroupResponse();
    expectedResponse.setError(new Error(InviteToGroupResponse.INVITED_USER_INVALID_ERROR));
    inviteUserToGroup(user1, invalidUser, user1Group.getId())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void invite_invalidGroup_shouldNotSucceed() throws Exception {
    Group unSavedGroup = GroupFactory.getNewGroup(user1.getId());

    InviteToGroupResponse expectedResponse = new InviteToGroupResponse();
    expectedResponse.setError(new Error(InviteToGroupResponse.GROUP_INVALID_ERROR));
    inviteUserToGroup(user1, user2, unSavedGroup.getId())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    User user2InRepo = userRepo.findById(user2.getId()).get();
    List<UserGroup> user2Groups = user2InRepo.getUserGroups();
    assertThat(user2Groups.size(), is(0));
    Group user1GroupInRepo = groupRepo.findById(user1Group.getId()).get();
    assertThat(user1GroupInRepo.getUsersIds().size(), is(1));
  }

  @Test
  public void invite_otherUserAlreadyMember_shouldNotSucceed() throws Exception {
    addUserToGroup(user2, /*invitingUser=*/user1, user1Group.getId());

    InviteToGroupResponse expectedResponse = new InviteToGroupResponse();
    expectedResponse.setError(new Error(InviteToGroupResponse.INVITED_USER_ALREADY_MEMBER_ERROR));
    inviteUserToGroup(user1, user2, user1Group.getId())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

  }

  @Test
  public void invite_invitingUserIsNotMember_shouldNotSucceed() throws Exception {
    InviteToGroupResponse expectedResponse = new InviteToGroupResponse();
    expectedResponse.setError(new Error(InviteToGroupResponse.INVITING_USER_IS_NOT_MEMBER_ERROR));
    inviteUserToGroup(user2, user3, user1Group.getId())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    User user3InRepo = userRepo.findById(user3.getId()).get();
    List<UserGroup> user3Groups = user3InRepo.getUserGroups();
    assertThat(user3Groups.size(), is(0));
  }

  @Test
  public void invite_invitingUserAlreadyInvitedOtherUser_shouldNotSucceed() throws Exception {
    InviteToGroupResponse expectedResponse = new InviteToGroupResponse();
    inviteUserToGroup(user1, user2, user1Group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    expectedResponse = new InviteToGroupResponse();
    expectedResponse.setError(new Error(InviteToGroupResponse.USER_ALREADY_INVITED_ERROR));
    inviteUserToGroup(user1, user2, user1Group.getId())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

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
    // Make user2 a member of the group.
    user1Group.getUsersIds().add(user2.getId());
    groupRepo.save(user1Group);

    InviteToGroupResponse expectedResponse = new InviteToGroupResponse();
    inviteUserToGroup(user1, user3, user1Group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    expectedResponse = new InviteToGroupResponse();
    inviteUserToGroup(user2, user3, user1Group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

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
    // user1 invites user2.
    InviteToGroupResponse expectedInviteToGroupResponse = new InviteToGroupResponse();
    inviteUserToGroup(user1, user2, user1Group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedInviteToGroupResponse)));

    // user2 accepts invitation.
    AcceptGroupInvitationResponse expectedAcceptGroupInvitationResponse =
        new AcceptGroupInvitationResponse();
    acceptInvitationToGroup(user2, user1Group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedAcceptGroupInvitationResponse)));

    User user2InRepo = userRepo.findById(user2.getId()).get();
    List<UserGroup> user2Groups = user2InRepo.getUserGroups();
    assertThat(user2Groups.size(), is(1));

    UserGroup user2Group = user2Groups.get(0);
    assertThat(user2Group.getGroupId(), is(user1Group.getId()));
    assertThat(user2Group.isPending(), is(false));

    Group user1GroupInRepo = groupRepo.findById(user1Group.getId()).get();
    assertThat(user1GroupInRepo.getUsersIds().size(), is(2));
  }

  @Test
  public void accept_invalidGroup_shouldNotSucceed() throws Exception {
    Group unSavedGroup = GroupFactory.getNewGroup(user1.getId());

    AcceptGroupInvitationResponse expectedResponse = new AcceptGroupInvitationResponse();
    expectedResponse.setError(new Error(InviteToGroupResponse.GROUP_INVALID_ERROR));
    inviteUserToGroup(user1, user2, unSavedGroup.getId())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    User user2InRepo = userRepo.findById(user2.getId()).get();
    List<UserGroup> user2Groups = user2InRepo.getUserGroups();
    assertThat(user2Groups.size(), is(0));

    Group user1GroupInRepo = groupRepo.findById(user1Group.getId()).get();
    assertThat(user1GroupInRepo.getUsersIds().size(), is(1));
  }

  @Test
  public void accept_notInvited_shouldNotSucceed() throws Exception {
    AcceptGroupInvitationResponse expectedResponse =
        new AcceptGroupInvitationResponse();
    expectedResponse.setError(new Error(AcceptGroupInvitationResponse.USER_NOT_INVITED_ERROR));
    acceptInvitationToGroup(user2, user1Group.getId())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    User user2InRepo = userRepo.findById(user2.getId()).get();
    List<UserGroup> user2Groups = user2InRepo.getUserGroups();
    assertThat(user2Groups.size(), is(0));

    Group user1GroupInRepo = groupRepo.findById(user1Group.getId()).get();
    assertThat(user1GroupInRepo.getUsersIds().size(), is(1));
  }

  @Test
  public void accept_AlreadyMember_shouldNotSucceed() throws Exception {
    // user1 invites user2.
    InviteToGroupResponse expectedInviteToGroupResponse = new InviteToGroupResponse();
    inviteUserToGroup(user1, user2, user1Group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedInviteToGroupResponse)));

    // user2 accepts group invitation.
    AcceptGroupInvitationResponse expectedAcceptGroupInvitationResponse =
        new AcceptGroupInvitationResponse();
    acceptInvitationToGroup(user2, user1Group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedAcceptGroupInvitationResponse)));

    expectedAcceptGroupInvitationResponse =
        new AcceptGroupInvitationResponse();
    // user2 accepts group invitation again.
    expectedAcceptGroupInvitationResponse
        .setError(new Error(AcceptGroupInvitationResponse.USER_ALREADY_MEMBER_ERROR));
    acceptInvitationToGroup(user2, user1Group.getId())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedAcceptGroupInvitationResponse)));

    User user2InRepo = userRepo.findById(user2.getId()).get();
    List<UserGroup> user2Groups = user2InRepo.getUserGroups();
    assertThat(user2Groups.size(), is(1));

    UserGroup user2Group = user2Groups.get(0);
    assertThat(user2Group.getGroupId(), is(user1Group.getId()));
    assertThat(user2Group.isPending(), is(false));

    Group user1GroupInRepo = groupRepo.findById(user1Group.getId()).get();
    assertThat(user1GroupInRepo.getUsersIds().size(), is(2));
  }

  @Test
  public void reject_normalScenario_shouldSucceed() throws Exception {
    // user1 invites user2.
    InviteToGroupResponse expectedInviteToGroupResponse = new InviteToGroupResponse();
    inviteUserToGroup(user1, user2, user1Group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedInviteToGroupResponse)));

    // user2 rejects invitation.
    RejectGroupInvitationResponse expectedAcceptGroupInvitationResponse =
        new RejectGroupInvitationResponse();
    rejectInvitationToGroup(user2, user1Group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedAcceptGroupInvitationResponse)));

    User user2InRepo = userRepo.findById(user2.getId()).get();
    List<UserGroup> user2Groups = user2InRepo.getUserGroups();
    assertThat(user2Groups.size(), is(0));

    Group group = groupRepo.findById(user1Group.getId()).get();
    assertThat(group.getUsersIds().size(), is(1));
  }

  @Test
  public void reject_notInvited_shouldNotSucceed() throws Exception {
    RejectGroupInvitationResponse expectedResponse =
        new RejectGroupInvitationResponse();
    expectedResponse.setError(new Error(RejectGroupInvitationResponse.USER_NOT_INVITED_ERROR));
    rejectInvitationToGroup(user2, user1Group.getId())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    User user2InRepo = userRepo.findById(user2.getId()).get();
    List<UserGroup> user2Groups = user2InRepo.getUserGroups();
    assertThat(user2Groups.size(), is(0));

    Group user1GroupInRepo = groupRepo.findById(user1Group.getId()).get();
    assertThat(user1GroupInRepo.getUsersIds().size(), is(1));
  }

  @Test
  public void reject_alreadyMember_shouldNotSucceed() throws Exception {
    addUserToGroup(user2, /*invitingUser=*/user1, user1Group.getId());

    RejectGroupInvitationResponse expectedRejectGroupInvitationResponse =
        new RejectGroupInvitationResponse();
    expectedRejectGroupInvitationResponse
        .setError(new Error(AcceptGroupInvitationResponse.USER_ALREADY_MEMBER_ERROR));
    rejectInvitationToGroup(user2, user1Group.getId())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedRejectGroupInvitationResponse)));

    User user2InRepo = userRepo.findById(user2.getId()).get();
    List<UserGroup> user2Groups = user2InRepo.getUserGroups();
    assertThat(user2Groups.size(), is(1));

    UserGroup user2Group = user2Groups.get(0);
    assertThat(user2Group.getGroupId(), is(user1Group.getId()));
    assertThat(user2Group.isPending(), is(false));

    Group user1GroupInRepo = groupRepo.findById(user1Group.getId()).get();
    assertThat(user1GroupInRepo.getUsersIds().size(), is(2));
  }

  @Test
  public void leave_normalScenario_shouldSucceed() throws Exception {
    addUserToGroup(user2, /*invitingUser=*/user1, user1Group.getId());

    LeaveGroupResponse expectedResponse = new LeaveGroupResponse();
    leaveGroup(user2, user1Group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

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
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void leave_notMember_shouldNoSucceed() throws Exception {
    LeaveGroupResponse expectedResponse = new LeaveGroupResponse();
    expectedResponse.setError(new Error(LeaveGroupResponse.NOT_MEMBER_ERROR));
    leaveGroup(user2, user1Group.getId())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void getGroups_normalScenario_shouldSucceed() throws Exception {
    String user1Group1Name = "user1group1name";
    String user1Group2Name = "user1group2name";
    String user2Group1Name = "user2group1name";
    Group group1 = azkarApi.addGroupAndReturn(user1, user1Group1Name);
    Group group2 = azkarApi.addGroupAndReturn(user1, user1Group2Name);
    Group group3 = azkarApi.addGroupAndReturn(user2, user2Group1Name);

    addUserToGroup(user3, /*invitingUser=*/user1, group1.getId());

    // Invite user3 to group2.
    inviteUserToGroup(user1, user3, group2.getId());

    addUserToGroup(user3, /*invitingUser=*/user2, group3.getId());

    GetUserGroupsResponse expectedResponse = new GetUserGroupsResponse();
    List<UserGroup> expectedUserGroups = new ArrayList();
    expectedUserGroups.add(UserGroup.builder()
        .groupId(group1.getId())
        .groupName(user1Group1Name)
        .isPending(false)
        .build());

    expectedUserGroups.add(UserGroup.builder()
        .groupId(group2.getId())
        .invitingUserId(user1.getId())
        .groupName(user1Group2Name)
        .isPending(true)
        .build());

    expectedUserGroups.add(UserGroup.builder()
        .groupId(group3.getId())
        .groupName(user2Group1Name)
        .isPending(false)
        .build());

    expectedResponse.setData(expectedUserGroups);
    performGetRequest(user3, "/groups/")
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)))
        .andReturn();
  }

  @Test
  public void getGroups_ownedGroups_normalScenario_shouldSucceed() throws Exception {
    String groupName1 = "group1name";
    String groupName2 = "group2name";
    Group group1 = azkarApi.addGroupAndReturn(user1, groupName1);
    Group group2 = azkarApi.addGroupAndReturn(user2, groupName2);

    GetUserGroupsResponse expectedUser1Response = new GetUserGroupsResponse();
    List<UserGroup> expectedUser1Groups = new ArrayList();
    expectedUser1Groups.add(UserGroup.builder()
        .groupId(group1.getId())
        .groupName(groupName1)
        .isPending(false)
        .build());
    expectedUser1Response.setData(expectedUser1Groups);

    GetUserGroupsResponse expectedUser2Response = new GetUserGroupsResponse();
    List<UserGroup> expectedUser2Groups = new ArrayList();
    expectedUser2Groups.add(UserGroup.builder()
        .groupId(group2.getId())
        .groupName(groupName2)
        .isPending(false)
        .build());
    expectedUser2Response.setData(expectedUser2Groups);

    azkarApi.getGroups(user1)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedUser1Response)))
        .andReturn();
    azkarApi.getGroups(user2)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedUser2Response)))
        .andReturn();
  }

  private ResultActions inviteUserToGroup(User invitingUser, User invitedUser, String groupId)
      throws Exception {
    return performPutRequest(invitingUser, String.format("/groups/%s/invite/%s", groupId,
        invitedUser.getId()),
        /*body=*/ null);
  }

  private ResultActions acceptInvitationToGroup(User user, String groupId)
      throws Exception {
    return performPutRequest(user, String.format("/groups/%s/accept/", groupId), /*body=*/ null);
  }

  private ResultActions rejectInvitationToGroup(User user, String groupId)
      throws Exception {
    return performPutRequest(user, String.format("/groups/%s/reject/", groupId), /*body=*/ null);
  }

  private ResultActions leaveGroup(User user, String groupId) throws Exception {
    return performPutRequest(user, String.format("/groups/%s/leave/", groupId), /*body=*/ null);
  }

  private void addUserToGroup(User user, User invitingUser, String groupId)
      throws Exception {
    inviteUserToGroup(invitingUser, user, groupId);
    acceptInvitationToGroup(user, groupId);
  }
}
