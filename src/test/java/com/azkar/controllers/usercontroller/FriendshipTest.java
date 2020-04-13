package com.azkar.controllers.usercontroller;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.controllers.ControllerTestBase;
import com.azkar.entities.Friendship;
import com.azkar.entities.Friendship.Friend;
import com.azkar.entities.User;
import com.azkar.factories.UserFactory;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.usercontroller.AddFriendResponse;
import com.azkar.payload.usercontroller.DeleteFriendResponse;
import com.azkar.payload.usercontroller.GetFriendsResponse;
import com.azkar.payload.usercontroller.ResolveFriendRequestResponse;
import com.azkar.repos.FriendshipRepo;
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

public class FriendshipTest extends ControllerTestBase {

  private static User user1 = UserFactory.getNewUser();
  private static User user2 = UserFactory.getNewUser();
  private static User user3 = UserFactory.getNewUser();
  private static User user4 = UserFactory.getNewUser();
  private static User user5 = UserFactory.getNewUser();
  private static User unAuthenticatedUser = UserFactory.getNewUser();

  @Autowired
  FriendshipRepo friendshipRepo;

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

    sendFriendRequest(user1, user2)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));

    Friendship user1Friendship = friendshipRepo.findByUserId(user1.getId());
    Friendship user2Friendship = friendshipRepo.findByUserId(user2.getId());

    assertThat(user1Friendship.getFriends().size(), is(0));
    assertThat(user2Friendship.getFriends().size(), is(1));

    Friend user2Friend = user2Friendship.getFriends().get(0);
    assertThat(user2Friend.getUserId(), equalTo(user1.getId()));
    assertThat(user2Friend.getUsername(), equalTo(user1.getUsername()));
    assertThat(user2Friend.isPending(), is(true));
  }

  @Test
  public void addFriend_requesterRequestedBefore_shouldNotSucceed() throws Exception {
    sendFriendRequest(user1, user2);

    AddFriendResponse expectedResponse = new AddFriendResponse();
    expectedResponse.setError(new Error(AddFriendResponse.ERROR_FRIENDSHIP_ALREADY_REQUESTED));

    sendFriendRequest(user1, user2)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
  }


  @Test
  public void addFriend_responderRequestedBefore_shouldAddNonPendingFriendship() throws Exception {
    sendFriendRequest(user1, user2);

    AddFriendResponse expectedResponse = new AddFriendResponse();
    sendFriendRequest(user2, user1)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));

    Friendship user1Friendship = friendshipRepo.findByUserId(user1.getId());
    Friendship user2Friendship = friendshipRepo.findByUserId(user2.getId());

    assertThat(user1Friendship.getFriends().size(), is(1));
    assertThat(user2Friendship.getFriends().size(), is(1));

    Friend user1Friend = user1Friendship.getFriends().get(0);
    assertThat(user1Friend.getUserId(), equalTo(user2.getId()));
    assertThat(user1Friend.getUsername(), equalTo(user2.getUsername()));
    assertThat(user1Friend.isPending(), is(false));

    Friend user2Friend = user2Friendship.getFriends().get(0);
    assertThat(user2Friend.getUserId(), equalTo(user1.getId()));
    assertThat(user2Friend.getUsername(), equalTo(user1.getUsername()));
    assertThat(user2Friend.isPending(), is(false));
  }


  @Test
  public void addFriend_invalidResponder_shouldNotSucceed() throws Exception {
    AddFriendResponse expectedResponse = new AddFriendResponse();
    expectedResponse.setError(new Error(AddFriendResponse.ERROR_USER_NOT_FOUND));

    sendFriendRequest(user1, unAuthenticatedUser)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
  }

  @Test
  public void acceptFriendRequest_normalScenario_shouldSucceed() throws Exception {
    sendFriendRequest(user1, user2);

    ResolveFriendRequestResponse expectedResponse = new ResolveFriendRequestResponse();
    acceptFriendRequest(user2, user1)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));

    Friendship user1Friendship = friendshipRepo.findByUserId(user1.getId());
    Friendship user2Friendship = friendshipRepo.findByUserId(user2.getId());

    assertThat(user1Friendship.getFriends().size(), is(1));
    assertThat(user2Friendship.getFriends().size(), is(1));

    Friend user1Friend = user1Friendship.getFriends().get(0);
    assertThat(user1Friend.getUserId(), equalTo(user2.getId()));
    assertThat(user1Friend.getUsername(), equalTo(user2.getUsername()));
    assertThat(user1Friend.isPending(), is(false));

    Friend user2Friend = user2Friendship.getFriends().get(0);
    assertThat(user2Friend.getUserId(), equalTo(user1.getId()));
    assertThat(user2Friend.getUsername(), equalTo(user1.getUsername()));
    assertThat(user2Friend.isPending(), is(false));
  }

  @Test
  public void rejectFriendRequest_normalScenario_shouldSucceed() throws Exception {
    sendFriendRequest(user1, user2);

    ResolveFriendRequestResponse expectedResponse = new ResolveFriendRequestResponse();
    rejectFriendRequest(user2, user1)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));

    Friendship user1Friendship = friendshipRepo.findByUserId(user1.getId());
    Friendship user2Friendship = friendshipRepo.findByUserId(user2.getId());

    assertThat(user1Friendship.getFriends().size(), is(0));
    assertThat(user2Friendship.getFriends().size(), is(0));
  }

  @Test
  public void acceptFriendRequest_friendshipNotPending_shouldNotSucceed() throws Exception {
    ResolveFriendRequestResponse expectedResponse = new ResolveFriendRequestResponse();
    expectedResponse
        .setError(new Error(ResolveFriendRequestResponse.ERROR_NO_FRIEND_REQUEST_EXIST));
    acceptFriendRequest(user1, user2)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));

    Friendship user1Friendship = friendshipRepo.findByUserId(user1.getId());
    Friendship user2Friendship = friendshipRepo.findByUserId(user2.getId());

    assertThat(user1Friendship.getFriends().size(), is(0));
    assertThat(user2Friendship.getFriends().size(), is(0));
  }

  @Test
  public void resolveFriendship_friendshipAlreadyExists_shouldNotSucceed() throws Exception {
    sendFriendRequest(user1, user2);
    acceptFriendRequest(user2, user1);

    ResolveFriendRequestResponse expectedResponse = new ResolveFriendRequestResponse();
    expectedResponse
        .setError(new Error(ResolveFriendRequestResponse.ERROR_FRIEND_REQUEST_ALREADY_ACCEPTED));
    acceptFriendRequest(user1, user2)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));

    rejectFriendRequest(user1, user2)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));

    expectedResponse
        .setError(new Error(ResolveFriendRequestResponse.ERROR_FRIEND_REQUEST_ALREADY_ACCEPTED));
    acceptFriendRequest(user2, user1)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));

    rejectFriendRequest(user2, user1)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
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
        .isPending(false)
        .build()
    );
    expectedUser1Friends.add(Friend.builder()
        .userId(user3.getId())
        .username(user3.getUsername())
        .isPending(false)
        .build()
    );

    authenticate(user1);
    MvcResult mvcResult =
        prepareGetRequest("/friends")
            .andExpect(status().isOk())
            .andReturn();

    GetFriendsResponse getUser1FriendsResponse =
        mapFromJson(mvcResult.getResponse().getContentAsString(), GetFriendsResponse.class);

    compareFriendshipList(getUser1FriendsResponse.getData().getFriends(), expectedUser1Friends);

    // user5 expected friends.
    List<Friend> expectedUser5Friends = new ArrayList();
    expectedUser5Friends.add(Friend.builder()
        .userId(user1.getId())
        .username(user1.getUsername())
        .isPending(true)
        .build()
    );
    authenticate(user5);
    mvcResult =
        prepareGetRequest("/friends")
            .andExpect(status().isOk())
            .andReturn();

    GetFriendsResponse getUser5FriendsResponse =
        mapFromJson(mvcResult.getResponse().getContentAsString(), GetFriendsResponse.class);

    compareFriendshipList(getUser5FriendsResponse.getData().getFriends(), expectedUser5Friends);

  }

  @Test
  public void deleteFriend_normalScenario_shouldSucceed() throws Exception {
    makeFriends(user1, user2);

    DeleteFriendResponse expectedResponse = new DeleteFriendResponse();
    deleteFriend(user1, user2)
        .andExpect(status().isNoContent())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));

    Friendship user1Friendship = friendshipRepo.findByUserId(user1.getId());
    Friendship user2Friendship = friendshipRepo.findByUserId(user2.getId());

    assertThat(user1Friendship.getFriends().size(), is(0));
    assertThat(user2Friendship.getFriends().size(), is(0));
  }

  @Test
  public void deleteFriend_friendRequestIsPending_shouldNotSucceed() throws Exception {
    sendFriendRequest(user1, user2);

    DeleteFriendResponse expectedResponse = new DeleteFriendResponse();
    expectedResponse.setError(new Error(DeleteFriendResponse.ERROR_NO_FRIENDSHIP));
    deleteFriend(user1, user2)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
  }

  @Test
  public void deleteFriend_noFriendRequestExist_shouldNotSucceed() throws Exception {
    DeleteFriendResponse expectedResponse = new DeleteFriendResponse();
    expectedResponse.setError(new Error(DeleteFriendResponse.ERROR_NO_FRIENDSHIP));

    deleteFriend(user1, user2)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
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
    authenticate(requester);
    return preparePutRequest(String.format("/friends/%s", responder.getId()),
        /*body=*/ null);
  }

  private ResultActions acceptFriendRequest(User responder, User requester) throws Exception {
    authenticate(responder);
    return preparePutRequest(String.format("/friends/%s/accept", requester.getId()),
        /*body=*/null);
  }

  private ResultActions rejectFriendRequest(User responder, User requester) throws Exception {
    authenticate(responder);
    return preparePutRequest(String.format("/friends/%s/reject", requester.getId()),
        /*body=*/null);
  }

  private ResultActions deleteFriend(User requester, User otherUser) throws Exception {
    authenticate(requester);
    return prepareDeleteRequest(String.format("/friends/%s", otherUser.getId()));
  }

  private void makeFriends(User user1, User user2) throws Exception {
    sendFriendRequest(user1, user2);
    acceptFriendRequest(user2, user1);
  }
}
