package com.azkar.controllers.groupcontroller;

import static org.hamcrest.CoreMatchers.equalTo;
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
import com.azkar.payload.ResponseBase.Status;
import com.azkar.payload.groupcontroller.responses.AddGroupResponse;
import com.azkar.payload.groupcontroller.responses.AddToGroupResponse;
import com.azkar.payload.groupcontroller.responses.GetGroupLeaderboardResponse;
import com.azkar.payload.groupcontroller.responses.GetGroupResponse;
import com.azkar.payload.groupcontroller.responses.GetGroupsResponse;
import com.azkar.payload.groupcontroller.responses.GetUserGroupsResponse;
import com.azkar.payload.groupcontroller.responses.LeaveGroupResponse;
import com.azkar.payload.utils.UserScore;
import com.azkar.repos.FriendshipRepo;
import com.azkar.repos.GroupRepo;
import com.azkar.repos.UserRepo;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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

  @Autowired
  FriendshipRepo friendshipRepo;

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
    long groupRepoCountBeforeOperation = groupRepo.count();

    MvcResult result =
        azkarApi.addGroup(user1, TEST_GROUP_NAME).andExpect(status().isOk()).andReturn();

    AddGroupResponse response = JsonHandler.fromJson(result.getResponse().getContentAsString(),
        AddGroupResponse.class);
    Group returnedGroup = response.getData();
    assertThat(returnedGroup.getName(), is(TEST_GROUP_NAME));
    assertThat(groupRepo.count(), is(groupRepoCountBeforeOperation + 1));
    assertThat(groupRepo.findById(returnedGroup.getId()).isPresent(), is(true));
  }

  @Test
  public void addUser_normalScenario_shouldSucceed() throws Exception {
    AddToGroupResponse expectedResponse = new AddToGroupResponse();

    azkarApi.makeFriends(user1, user2);

    int user2GroupsNumBefore = userRepo.findById(user2.getId()).get().getUserGroups().size();

    azkarApi.addUserToGroup(/*invitingUser=*/user1, user2, user1Group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    User user2InRepo = userRepo.findById(user2.getId()).get();
    List<UserGroup> user2Groups = user2InRepo.getUserGroups();
    assertThat(user2Groups.size(), is(user2GroupsNumBefore + 1));

    UserGroup user2Group = user2Groups.get(user2GroupsNumBefore + 0);
    assertThat(user2Group.getGroupId(), is(user1Group.getId()));
    assertThat(user2Group.getInvitingUserId(), is(user1.getId()));

    Group updatedGroup = groupRepo.findById(user1Group.getId()).get();
    assertThat(updatedGroup.getUsersIds().size(), is(2));
    assertThat(updatedGroup.getUsersIds().get(0), equalTo(user1.getId()));
    assertThat(updatedGroup.getUsersIds().get(1), equalTo(user2.getId()));
  }

  @Test
  public void addUser_notFriend_shouldFail() throws Exception {
    AddToGroupResponse expectedResponse = new AddToGroupResponse();
    expectedResponse.setStatus(new Status(Status.NO_FRIENDSHIP_ERROR));
    azkarApi.addUserToGroup(/*invitingUser=*/user1, user2, user1Group.getId())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void addUser_invalidOtherUser_shouldNotSucceed() throws Exception {
    AddToGroupResponse expectedResponse = new AddToGroupResponse();
    expectedResponse.setStatus(new Status(Status.INVITED_USER_INVALID_ERROR));
    azkarApi.addUserToGroup(/*invitingUser=*/user1, invalidUser, user1Group.getId())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void addUser_invalidGroup_shouldNotSucceed() throws Exception {
    Group unSavedGroup = GroupFactory.getNewGroup(user1.getId());

    AddToGroupResponse expectedResponse = new AddToGroupResponse();
    expectedResponse.setStatus(new Status(Status.GROUP_INVALID_ERROR));
    azkarApi.addUserToGroup(/*invitingUser=*/user1, user2, unSavedGroup.getId())
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
  public void addUser_otherUserAlreadyMember_shouldNotSucceed() throws Exception {
    azkarApi.makeFriends(user1, user2);

    azkarApi.addUserToGroup(/*invitingUser=*/user1, user2, user1Group.getId());

    AddToGroupResponse expectedResponse = new AddToGroupResponse();
    expectedResponse.setStatus(new Status(Status.INVITED_USER_ALREADY_MEMBER_ERROR));

    azkarApi.addUserToGroup(/*invitingUser=*/user1, user2, user1Group.getId())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void addUser_invitingUserIsNotMember_shouldNotSucceed() throws Exception {
    AddToGroupResponse expectedResponse = new AddToGroupResponse();
    expectedResponse.setStatus(new Status(Status.INVITING_USER_IS_NOT_MEMBER_ERROR));

    azkarApi.makeFriends(user2, user3);

    int user3NumGroupsBefore = userRepo.findById(user3.getId()).get().getUserGroups().size();

    azkarApi.addUserToGroup(/*invitingUser=*/user2, user3, user1Group.getId())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    User user3InRepo = userRepo.findById(user3.getId()).get();
    List<UserGroup> user3Groups = user3InRepo.getUserGroups();
    assertThat(user3Groups.size(), is(user3NumGroupsBefore));
  }

  @Test
  public void leave_normalScenario_shouldSucceed() throws Exception {
    azkarApi.makeFriends(user1, user2);

    azkarApi.addUserToGroup(/*invitingUser=*/user1, user2, user1Group.getId());

    int user2NumGroupsBefore = userRepo.findById(user2.getId()).get().getUserGroups().size();
    LeaveGroupResponse expectedResponse = new LeaveGroupResponse();
    leaveGroup(user2, user1Group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    Group group = groupRepo.findById(user1Group.getId()).get();
    assertThat(group.getUsersIds().size(), is(1));
    assertThat(group.getUsersIds().get(0), is(user1.getId()));

    User user2InRepo = userRepo.findById(user2.getId()).get();
    assertThat(user2InRepo.getUserGroups().size(), is(user2NumGroupsBefore - 1));
  }

  @Test
  public void leave_invalidGroup_shouldNoSucceed() throws Exception {
    Group unSavedGroup = GroupFactory.getNewGroup(user1.getId());

    LeaveGroupResponse expectedResponse = new LeaveGroupResponse();
    expectedResponse.setStatus(new Status(Status.GROUP_INVALID_ERROR));
    leaveGroup(user2, unSavedGroup.getId())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void leave_notMember_shouldNoSucceed() throws Exception {
    LeaveGroupResponse expectedResponse = new LeaveGroupResponse();
    expectedResponse.setStatus(new Status(Status.NOT_MEMBER_ERROR));
    leaveGroup(user2, user1Group.getId())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void getUserGroups_normalScenario_shouldSucceed() throws Exception {
    User user4 = getNewRegisteredUser();

    azkarApi.makeFriends(user3, user1);
    azkarApi.makeFriends(user3, user2);

    String user1Group1Name = "user1group1name";
    String user1Group2Name = "user1group2name";
    String user2Group1Name = "user2group1name";
    String user4Group1Name = "user4group1name";
    Group group1 = azkarApi.addGroupAndReturn(user1, user1Group1Name);
    Group group2 = azkarApi.addGroupAndReturn(user1, user1Group2Name);
    Group group3 = azkarApi.addGroupAndReturn(user2, user2Group1Name);
    azkarApi.addGroupAndReturn(user4, user4Group1Name);

    GetUserGroupsResponse expectedResponse = new GetUserGroupsResponse();
    List<UserGroup> expectedUser3Groups =
        new ArrayList(userRepo.findById(user3.getId()).get().getUserGroups());
    expectedUser3Groups.add(UserGroup.builder()
        .groupId(group1.getId())
        .groupName(user1Group1Name)
        .build());

    expectedUser3Groups.add(UserGroup.builder()
        .groupId(group2.getId())
        .invitingUserId(user1.getId())
        .groupName(user1Group2Name)
        .build());

    expectedUser3Groups.add(UserGroup.builder()
        .groupId(group3.getId())
        .groupName(user2Group1Name)
        .build());

    expectedResponse.setData(expectedUser3Groups);

    // Add user3 to group1.
    azkarApi.addUserToGroup(/*invitingUser=*/user1, user3, group1.getId());

    // Add user3 to group2.
    azkarApi.addUserToGroup(/*invitingUser=*/user1, user3, group2.getId());

    // Add user3 to group3.
    azkarApi.addUserToGroup(/*invitingUser=*/user2, user3, group3.getId());

    azkarApi.getUserGroups(user3)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void getGroups_normalScenario_shouldSucceed() throws Exception {
    User user4 = getNewRegisteredUser();

    azkarApi.makeFriends(user3, user1);
    azkarApi.makeFriends(user3, user2);

    String user1Group1Name = "user1group1name";
    String user1Group2Name = "user1group2name";
    String user2Group1Name = "user2group1name";
    String user4Group1Name = "user4group1name";
    Group group1 = azkarApi.addGroupAndReturn(user1, user1Group1Name);
    Group group2 = azkarApi.addGroupAndReturn(user1, user1Group2Name);
    Group group3 = azkarApi.addGroupAndReturn(user2, user2Group1Name);
    azkarApi.addGroupAndReturn(user4, user4Group1Name);

    GetGroupsResponse expectedResponse = new GetGroupsResponse();
    // Add all currently existing groups that user 3 exist in before doing any of the operations
    // under test.
    List<Group> expectedUser3Groups =
        new ArrayList(groupRepo.findAll().stream()
            .filter(group -> group.getUsersIds().contains(user3.getId())).collect(
                Collectors.toList()));
    expectedUser3Groups.add(group1.toBuilder()
        .usersIds(ImmutableList.of(user1.getId(), user3.getId())).build());

    expectedUser3Groups.add(group2.toBuilder()
        .usersIds(ImmutableList.of(user1.getId(), user3.getId())).build());

    expectedUser3Groups.add(group3.toBuilder()
        .usersIds(ImmutableList.of(user2.getId(), user3.getId())).build());

    expectedResponse.setData(expectedUser3Groups);

    // Add user3 to group1.
    azkarApi.addUserToGroup(/*invitingUser=*/user1, user3, group1.getId());

    // Add user3 to group2.
    azkarApi.addUserToGroup(/*invitingUser=*/user1, user3, group2.getId());

    // Add user3 to group3.
    azkarApi.addUserToGroup(/*invitingUser=*/user2, user3, group3.getId());

    azkarApi.getGroups(user3)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void getGroup_normalScenario_shouldSucceed() throws Exception {
    azkarApi.addGroup(user1, "group1");
    Group group2 = azkarApi.addGroupAndReturn(user1, "group2");
    azkarApi.addGroup(user2, "group3");

    GetGroupResponse expectedResponse = new GetGroupResponse();
    expectedResponse.setData(group2);
    azkarApi.getGroup(user1, group2.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)))
        .andReturn();
  }

  @Test
  public void getGroup_notMember_shouldFail() throws Exception {
    azkarApi.addGroup(user1, "group1");
    azkarApi.addGroup(user1, "group2");
    Group group3 = azkarApi.addGroupAndReturn(user2, "group3");

    GetGroupResponse expectedResponse = new GetGroupResponse();
    expectedResponse.setStatus(new Status(Status.NOT_MEMBER_IN_GROUP_ERROR));
    azkarApi.getGroup(user1, group3.getId())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)))
        .andReturn();
  }

  @Test
  public void getGroup_notExistingGroup_shouldFail() throws Exception {
    azkarApi.addGroup(user1, "group1");
    azkarApi.addGroup(user1, "group2");
    azkarApi.addGroup(user2, "group3");

    GetGroupResponse expectedResponse = new GetGroupResponse();
    expectedResponse.setStatus(new Status(Status.NOT_MEMBER_IN_GROUP_ERROR));
    azkarApi.getGroup(user1, "nonExistingGroupId")
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)))
        .andReturn();
  }

  @Test
  public void getUserGroups_ownedGroups_normalScenario_shouldSucceed() throws Exception {
    String groupName1 = "group1name";
    String groupName2 = "group2name";
    Group group1 = azkarApi.addGroupAndReturn(user1, groupName1);
    Group group2 = azkarApi.addGroupAndReturn(user2, groupName2);

    GetUserGroupsResponse expectedUser1Response = new GetUserGroupsResponse();
    List<UserGroup> expectedUser1Groups = new ArrayList();
    expectedUser1Groups.add(UserGroup.builder()
        .groupId(group1.getId())
        .groupName(groupName1)
        .build());
    expectedUser1Response.setData(expectedUser1Groups);

    GetUserGroupsResponse expectedUser2Response = new GetUserGroupsResponse();
    List<UserGroup> expectedUser2Groups = new ArrayList();
    expectedUser2Groups.add(UserGroup.builder()
        .groupId(group2.getId())
        .groupName(groupName2)
        .build());
    expectedUser2Response.setData(expectedUser2Groups);

    azkarApi.getUserGroups(user1)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedUser1Response)))
        .andReturn();
    azkarApi.getUserGroups(user2)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedUser2Response)))
        .andReturn();
  }

  @Test
  public void getGroupLeaderboard_normalScenario_returnsSortedScores() throws Exception {
    User user1 = getNewRegisteredUser();
    User user2 = getNewRegisteredUser();
    azkarApi.makeFriends(user1, user2);

    Group group = azkarApi.addGroupAndReturn(user1, "group");
    int user1GroupIndex = getLastAddedUserGroupIndex(user1);
    azkarApi.addGroup(user1, "irrelevant group");
    int user1IrrelevantGroupIndex = getLastAddedUserGroupIndex(user1);
    azkarApi.addUserToGroup(/*invitingUser=*/user1, user2, group.getId());
    int user2GroupIndex = getLastAddedUserGroupIndex(user2);

    User user1InDb = userRepo.findById(user1.getId()).get();
    User user2InDb = userRepo.findById(user2.getId()).get();
    user1InDb.getUserGroups().get(user1GroupIndex).setTotalScore(5);
    user1InDb.getUserGroups().get(user1IrrelevantGroupIndex).setTotalScore(20);
    user2InDb.getUserGroups().get(user2GroupIndex).setTotalScore(10);
    userRepo.save(user1InDb);
    userRepo.save(user2InDb);
    GetGroupLeaderboardResponse expectedResponse = new GetGroupLeaderboardResponse();
    List<UserScore> expectedUserScores = new ArrayList<>();
    expectedUserScores.add(
        UserScore.builder().firstName(user2.getFirstName()).lastName(user2.getLastName())
            .username(user2.getUsername()).totalScore(10)
            .build());
    expectedUserScores.add(
        UserScore.builder().firstName(user1.getFirstName()).lastName(user1.getLastName())
            .username(user1.getUsername()).totalScore(5)
            .build());
    expectedResponse.setData(expectedUserScores);

    azkarApi.getGroupLeaderboard(user1, group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)))
        .andReturn();
    azkarApi.getGroupLeaderboard(user2, group.getId())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)))
        .andReturn();
  }

  @Test
  public void getGroupLeaderboard_binaryGroup_accountsForOtherGroupsScore() throws Exception {
    User user1 = getNewRegisteredUser();
    User user2 = getNewRegisteredUser();
    azkarApi.makeFriends(user1, user2);

    Group group1 = azkarApi.addGroupAndReturn(user1, "group1");
    int user1Group1Index = getLastAddedUserGroupIndex(user1);
    azkarApi.addUserToGroup(/*invitingUser=*/user1, user2, group1.getId());
    int user2Group1Index = getLastAddedUserGroupIndex(user2);

    Group group2 = azkarApi.addGroupAndReturn(user2, "group2");
    int user2Group2Index = getLastAddedUserGroupIndex(user2);
    azkarApi.addUserToGroup(/*invitingUser=*/user2, user1, group2.getId());
    int user1Group2Index = getLastAddedUserGroupIndex(user1);

    azkarApi.addGroup(user1, "irrelevant group");
    int user1IrrelevantGroupIndex = getLastAddedUserGroupIndex(user1);

    User user1InDb = userRepo.findById(user1.getId()).get();
    User user2InDb = userRepo.findById(user2.getId()).get();

    user1InDb.getUserGroups().get(user1Group1Index).setTotalScore(5);
    user1InDb.getUserGroups().get(user1Group2Index).setTotalScore(10);
    user1InDb.getUserGroups().get(user1IrrelevantGroupIndex).setTotalScore(15);

    user2InDb.getUserGroups().get(user2Group1Index).setTotalScore(50);
    user2InDb.getUserGroups().get(user2Group2Index).setTotalScore(100);

    userRepo.save(user1InDb);
    userRepo.save(user2InDb);
    GetGroupLeaderboardResponse expectedResponse = new GetGroupLeaderboardResponse();
    List<UserScore> expectedUserScores = new ArrayList<>();
    expectedUserScores.add(
        UserScore.builder().firstName(user2.getFirstName()).lastName(user2.getLastName())
            .username(user2.getUsername()).totalScore(150)
            .build());
    expectedUserScores.add(
        UserScore.builder().firstName(user1.getFirstName()).lastName(user1.getLastName())
            .username(user1.getUsername()).totalScore(15)
            .build());
    expectedResponse.setData(expectedUserScores);

    String binaryGroupId =
        friendshipRepo.findByUserId(user1.getId()).getFriends().get(0).getGroupId();
    azkarApi.getGroupLeaderboard(user1, binaryGroupId)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)))
        .andReturn();
    azkarApi.getGroupLeaderboard(user2, binaryGroupId)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)))
        .andReturn();
  }

  @Test
  public void getGroupLeaderboard_userNotInGroup_shouldFail() throws Exception {
    User user = UserFactory.getNewUser();
    addNewUser(user);
    User nonGroupUser = UserFactory.getNewUser();
    addNewUser(nonGroupUser);
    Group group = azkarApi.addGroupAndReturn(user, "group");

    User user1InDb = userRepo.findById(user.getId()).get();
    user1InDb.getUserGroups().get(0).setTotalScore(5);
    userRepo.save(user1InDb);

    GetGroupLeaderboardResponse expectedResponse = new GetGroupLeaderboardResponse();
    expectedResponse.setStatus(new Status(Status.NOT_MEMBER_IN_GROUP_ERROR));

    azkarApi.getGroupLeaderboard(nonGroupUser, group.getId())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)))
        .andReturn();
  }

  private ResultActions leaveGroup(User user, String groupId) throws Exception {
    return performPutRequest(user, String.format("/groups/%s/leave/", groupId), /*body=*/ null);
  }

  private int getLastAddedUserGroupIndex(User user1) {
    return userRepo.findById(user1.getId()).get().getUserGroups().size() - 1;
  }
}
