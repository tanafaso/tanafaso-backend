package com.azkar.controllers.friendshipcontroller;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.TestBase;
import com.azkar.controllers.utils.JsonHandler;
import com.azkar.entities.Friendship;
import com.azkar.entities.Friendship.Friend;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.factories.entities.UserFactory;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.usercontroller.AddFriendResponse;
import com.azkar.payload.usercontroller.DeleteFriendResponse;
import com.azkar.payload.usercontroller.GetFriendsResponse;
import com.azkar.payload.usercontroller.ResolveFriendRequestResponse;
import com.azkar.repos.FriendshipRepo;
import com.azkar.repos.GroupRepo;
import com.azkar.repos.UserRepo;
import com.google.common.collect.Iterators;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

public class FriendshipTest extends TestBase {

  private static User user1 = UserFactory.getNewUser();
  private static User user2 = UserFactory.getNewUser();
  private static User user3 = UserFactory.getNewUser();
  private static User user4 = UserFactory.getNewUser();
  private static User user5 = UserFactory.getNewUser();
  private static User unAuthenticatedUser = UserFactory.getNewUser();

  @Autowired
  FriendshipRepo friendshipRepo;

  @Autowired
  GroupRepo groupRepo;

  @Autowired
  UserRepo userRepo;

  @Before
  public void before() {
    addNewUser(user1);
    addNewUser(user2);
    addNewUser(user3);
    addNewUser(user4);
    addNewUser(user5);
  }

  @Test
  public void addFriend_normalScenario_shouldSucceed() throws Exception {
    AddFriendResponse expectedResponse = new AddFriendResponse();
    assertThat(groupRepo.count(), equalTo(0L));

    sendFriendRequest(user1, user2)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    Friendship user1Friendship = friendshipRepo.findByUserId(user1.getId());
    Friendship user2Friendship = friendshipRepo.findByUserId(user2.getId());

    assertThat(user1Friendship.getFriends().size(), is(0));
    assertThat(user2Friendship.getFriends().size(), is(1));

    Friend user2Friend = user2Friendship.getFriends().get(0);
    assertFriendship(user2Friend, user1, /*isPending=*/true);

    assertThat(groupRepo.count(), equalTo(0L));
  }

  @Test
  public void addFriend_requesterRequestedBefore_shouldNotSucceed() throws Exception {
    assertThat(groupRepo.count(), equalTo(0L));

    sendFriendRequest(user1, user2);

    AddFriendResponse expectedResponse = new AddFriendResponse();
    expectedResponse.setError(new Error(AddFriendResponse.FRIENDSHIP_ALREADY_REQUESTED_ERROR));

    sendFriendRequest(user1, user2)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    assertThat(groupRepo.count(), equalTo(0L));
  }


  @Test
  public void addFriend_responderRequestedBefore_shouldAddNonPendingFriendship() throws Exception {
    sendFriendRequest(user1, user2);

    AddFriendResponse expectedResponse = new AddFriendResponse();
    sendFriendRequest(user2, user1)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    Friendship user1Friendship = friendshipRepo.findByUserId(user1.getId());
    Friendship user2Friendship = friendshipRepo.findByUserId(user2.getId());

    assertThat(user1Friendship.getFriends().size(), is(1));
    assertThat(user2Friendship.getFriends().size(), is(1));

    Friend user1Friend = user1Friendship.getFriends().get(0);
    assertFriendship(user1Friend, user2, /*isPending=*/false);

    Friend user2Friend = user2Friendship.getFriends().get(0);
    assertFriendship(user2Friend, user1, /*isPending=*/false);
  }


  @Test
  public void addFriend_invalidResponder_shouldNotSucceed() throws Exception {
    AddFriendResponse expectedResponse = new AddFriendResponse();
    expectedResponse.setError(new Error(AddFriendResponse.USER_NOT_FOUND_ERROR));

    sendFriendRequest(user1, unAuthenticatedUser)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void addFriend_addSelf_shouldNotSucceed() throws Exception {
    AddFriendResponse expectedResponse = new AddFriendResponse();
    expectedResponse.setError(new Error(AddFriendResponse.ADD_SELF_ERROR));

    sendFriendRequest(user1, user1)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void acceptFriendRequest_normalScenario_shouldSucceed() throws Exception {
    assertThat(groupRepo.count(), equalTo(0L));
    assertThat(user1.getUserGroups().size(), equalTo(0));
    assertThat(user2.getUserGroups().size(), equalTo(0));

    sendFriendRequest(user1, user2);

    ResolveFriendRequestResponse expectedResponse = new ResolveFriendRequestResponse();
    acceptFriendRequest(user2, user1)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    Friendship user1Friendship = friendshipRepo.findByUserId(user1.getId());
    Friendship user2Friendship = friendshipRepo.findByUserId(user2.getId());

    assertThat(user1Friendship.getFriends().size(), is(1));
    assertThat(user2Friendship.getFriends().size(), is(1));

    Friend user1Friend = user1Friendship.getFriends().get(0);
    assertFriendship(user1Friend, user2, /*isPending=*/false);

    Friend user2Friend = user2Friendship.getFriends().get(0);
    assertFriendship(user2Friend, user1, /*isPending=*/false);

    assertThat(groupRepo.count(), equalTo(1L));
    Group group = Iterators.getOnlyElement(groupRepo.findAll().iterator());
    assertThat(group.getUsersIds().size(), equalTo(2));
    assertThat(group.getUsersIds().contains(user1.getId()), is(true));
    assertThat(group.getUsersIds().contains(user2.getId()), is(true));

    assertThat(user1Friend.getGroupId(), notNullValue());
    assertThat(user1Friend.getGroupId(), equalTo(user2Friend.getGroupId()));

    User updatedUser1 = userRepo.findById(user1.getId()).get();
    User updatedUser2 = userRepo.findById(user2.getId()).get();
    assertThat(Iterators.getOnlyElement(updatedUser1.getUserGroups().iterator()).getGroupId(),
        equalTo(group.getId()));
    assertThat(Iterators.getOnlyElement(updatedUser2.getUserGroups().iterator()).getGroupId(),
        equalTo(group.getId()));
  }

  @Test
  public void rejectFriendRequest_normalScenario_shouldSucceed() throws Exception {
    assertThat(groupRepo.count(), equalTo(0L));

    sendFriendRequest(user1, user2);

    ResolveFriendRequestResponse expectedResponse = new ResolveFriendRequestResponse();
    rejectFriendRequest(user2, user1)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    Friendship user1Friendship = friendshipRepo.findByUserId(user1.getId());
    Friendship user2Friendship = friendshipRepo.findByUserId(user2.getId());

    assertThat(user1Friendship.getFriends().size(), is(0));
    assertThat(user2Friendship.getFriends().size(), is(0));
    assertThat(groupRepo.count(), equalTo(0L));
  }

  @Test
  public void acceptFriendRequest_friendshipNotPending_shouldNotSucceed() throws Exception {
    ResolveFriendRequestResponse expectedResponse = new ResolveFriendRequestResponse();
    expectedResponse
        .setError(new Error(ResolveFriendRequestResponse.NO_FRIEND_REQUEST_EXIST_ERROR));
    acceptFriendRequest(user1, user2)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    Friendship user1Friendship = friendshipRepo.findByUserId(user1.getId());
    Friendship user2Friendship = friendshipRepo.findByUserId(user2.getId());

    assertThat(user1Friendship.getFriends().size(), is(0));
    assertThat(user2Friendship.getFriends().size(), is(0));
  }

  @Test
  public void resolveFriendship_noFriendshipExist_shouldNotSucceed() throws Exception {
    ResolveFriendRequestResponse expectedResponse = new ResolveFriendRequestResponse();
    expectedResponse
        .setError(new Error(ResolveFriendRequestResponse.NO_FRIEND_REQUEST_EXIST_ERROR));

    acceptFriendRequest(user1, user2)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    rejectFriendRequest(user2, user1)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void resolveFriendship_friendshipAlreadyExists_shouldNotSucceed() throws Exception {
    sendFriendRequest(user1, user2);
    acceptFriendRequest(user2, user1);

    ResolveFriendRequestResponse expectedResponse = new ResolveFriendRequestResponse();
    expectedResponse
        .setError(new Error(ResolveFriendRequestResponse.FRIEND_REQUEST_ALREADY_ACCEPTED_ERROR));
    acceptFriendRequest(user1, user2)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    rejectFriendRequest(user1, user2)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    acceptFriendRequest(user2, user1)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    rejectFriendRequest(user2, user1)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }


  @Test
  public void getFriends_normalScenario_shouldSucceed() throws Exception {
    sendFriendRequest(user1, user5);

    makeFriends(user1, user2);
    makeFriends(user3, user4);
    makeFriends(user3, user1);

    // user1 expected friends.
    List<Friend> expectedUser1Friends = new ArrayList();
    expectedUser1Friends.add(Friend.builder()
        .userId(user2.getId())
        .username(user2.getUsername())
        .name(user2.getName())
        .isPending(false)
        .build()
    );
    expectedUser1Friends.add(Friend.builder()
        .userId(user3.getId())
        .username(user3.getUsername())
        .name(user3.getName())
        .isPending(false)
        .build()
    );

    MvcResult mvcResult =
        performGetRequest(user1, "/friends")
            .andExpect(status().isOk())
            .andReturn();

    GetFriendsResponse getUser1FriendsResponse =
        JsonHandler
            .fromJson(mvcResult.getResponse().getContentAsString(), GetFriendsResponse.class);

    compareFriendshipList(getUser1FriendsResponse.getData().getFriends(), expectedUser1Friends);

    // user5 expected friends.
    List<Friend> expectedUser5Friends = new ArrayList();
    expectedUser5Friends.add(Friend.builder()
        .userId(user1.getId())
        .username(user1.getUsername())
        .name(user1.getName())
        .isPending(true)
        .build()
    );
    mvcResult = performGetRequest(user5, "/friends")
        .andExpect(status().isOk())
        .andReturn();

    GetFriendsResponse getUser5FriendsResponse =
        JsonHandler
            .fromJson(mvcResult.getResponse().getContentAsString(), GetFriendsResponse.class);

    compareFriendshipList(getUser5FriendsResponse.getData().getFriends(), expectedUser5Friends);

  }

  @Test
  public void deleteFriend_normalScenario_shouldSucceed() throws Exception {
    makeFriends(user1, user2);
    assertThat(groupRepo.count(), equalTo(1L));
    User updatedUser1 = userRepo.findById(user1.getId()).get();
    User updatedUser2 = userRepo.findById(user2.getId()).get();
    assertThat(updatedUser1.getUserGroups().size(), equalTo(1));
    assertThat(updatedUser2.getUserGroups().size(), equalTo(1));

    DeleteFriendResponse expectedResponse = new DeleteFriendResponse();
    deleteFriend(user1, user2)
        .andExpect(status().isNoContent())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));

    Friendship user1Friendship = friendshipRepo.findByUserId(user1.getId());
    Friendship user2Friendship = friendshipRepo.findByUserId(user2.getId());

    assertThat(user1Friendship.getFriends().size(), is(0));
    assertThat(user2Friendship.getFriends().size(), is(0));

    assertThat(groupRepo.count(), equalTo(0L));
    updatedUser1 = userRepo.findById(user1.getId()).get();
    updatedUser2 = userRepo.findById(user2.getId()).get();
    assertThat(updatedUser1.getUserGroups().size(), equalTo(0));
    assertThat(updatedUser2.getUserGroups().size(), equalTo(0));
  }

  @Test
  public void deleteFriend_invalidUser_shouldSucceed() throws Exception {
    DeleteFriendResponse expectedResponse = new DeleteFriendResponse();
    expectedResponse.setError(new Error(DeleteFriendResponse.USER_NOT_FOUND_ERROR));

    deleteFriend(user1, unAuthenticatedUser)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void deleteFriend_friendRequestIsPending_shouldNotSucceed() throws Exception {
    sendFriendRequest(user1, user2);

    DeleteFriendResponse expectedResponse = new DeleteFriendResponse();
    expectedResponse.setError(new Error(DeleteFriendResponse.NO_FRIENDSHIP_ERROR));
    deleteFriend(user1, user2)
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  @Test
  public void deleteFriend_noFriendRequestExist_shouldNotSucceed() throws Exception {
    DeleteFriendResponse expectedResponse = new DeleteFriendResponse();
    expectedResponse.setError(new Error(DeleteFriendResponse.NO_FRIENDSHIP_ERROR));

    deleteFriend(user1, user2)
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }

  private void compareFriendshipList(List<Friend> actual, List<Friend> expected) {
    Collections.sort(actual, Comparator.comparing(Friend::getUserId));
    Collections.sort(expected, Comparator.comparing(Friend::getUserId));

    assertThat(actual.size(), equalTo(expected.size()));

    for (int i = 0; i < actual.size(); i++) {
      Friend actualFriend = actual.get(i);
      Friend expectedFriend = actual.get(i);
      assertThat(actualFriend.getUserId(), equalTo(expectedFriend.getUserId()));
      assertThat(actualFriend.getUsername(), equalTo(expectedFriend.getUsername()));
      assertThat(actualFriend.isPending(), equalTo(expectedFriend.isPending()));
    }
  }

  private ResultActions sendFriendRequest(User requester, User responder) throws Exception {
    return performPutRequest(requester, String.format("/friends/%s", responder.getId()),
        /*body=*/ null);
  }

  private ResultActions acceptFriendRequest(User responder, User requester) throws Exception {
    return performPutRequest(responder, String.format("/friends/%s/accept", requester.getId()),
        /*body=*/null);
  }

  private ResultActions rejectFriendRequest(User responder, User requester) throws Exception {
    return performPutRequest(responder, String.format("/friends/%s/reject", requester.getId()),
        /*body=*/null);
  }

  private ResultActions deleteFriend(User requester, User otherUser) throws Exception {
    return performDeleteRequest(requester, String.format("/friends/%s", otherUser.getId()));
  }

  private void makeFriends(User user1, User user2) throws Exception {
    sendFriendRequest(user1, user2);
    acceptFriendRequest(user2, user1);
  }

  private void assertFriendship(Friend friend, User user, boolean isPending) {
    assertThat(friend.getUserId(), equalTo(user.getId()));
    assertThat(friend.getUsername(), equalTo(user.getUsername()));
    assertThat(friend.isPending(), is(isPending));
  }
}
