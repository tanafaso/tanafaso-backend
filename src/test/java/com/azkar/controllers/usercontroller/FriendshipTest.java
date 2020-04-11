package com.azkar.controllers.usercontroller;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.controllers.BaseControllerTest;
import com.azkar.entities.Friendship;
import com.azkar.entities.User;
import com.azkar.factories.UserFactory;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.usercontroller.AddFriendResponse;
import com.azkar.payload.usercontroller.DeleteFriendResponse;
import com.azkar.payload.usercontroller.GetFriendsResponse;
import com.azkar.payload.usercontroller.ResolveFriendRequestResponse;
import com.azkar.repos.FriendshipRepo;
import com.azkar.requestbodies.usercontroller.ResolveFriendRequestBody;
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

public class FriendshipTest extends BaseControllerTest {

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
    /*
     * Authenticate users to make sure they signed in at least once and exist in the database
     * before any friendship operation.
     */
    authenticate(user1);
    authenticate(user2);
    authenticate(user3);
    authenticate(user4);
    authenticate(user5);
  }

  @Test
  public void addFriend_normalScenario_shouldSucceed() throws Exception {
    AddFriendResponse expectedResponse = new AddFriendResponse();

    sendFriendRequest(user1, user2)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));

    assertThat(friendshipRepo.count(), is(1L));
    Friendship friendship = friendshipRepo.findAll().get(0);
    assertThat(friendship.getRequesterId(), equalTo(user1.getId()));
    assertThat(friendship.getResponderId(), equalTo(user2.getId()));
    assertThat(friendship.getRequesterUsername(), equalTo(user1.getUsername()));
    assertThat(friendship.getResponderUsername(), equalTo(user2.getUsername()));
    assertThat(friendship.isPending(), is(true));
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

    assertThat(friendshipRepo.count(), is(1L));
    Friendship friendship = friendshipRepo.findAll().get(0);
    assertThat(friendship.getRequesterId(), equalTo(user1.getId()));
    assertThat(friendship.getResponderId(), equalTo(user2.getId()));
    assertThat(friendship.isPending(), is(false));
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

    assertThat(friendshipRepo.count(), is(1L));
    Friendship friendship = friendshipRepo.findAll().get(0);
    assertThat(friendship.getRequesterId(), equalTo(user1.getId()));
    assertThat(friendship.getResponderId(), equalTo(user2.getId()));
    assertThat(friendship.isPending(), is(false));
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

    assertThat(friendshipRepo.count(), is(0L));
  }

  @Test
  public void resolveFriendship_friendshipAlreadyExists_shouldNotSucceed() throws Exception {
    sendFriendRequest(user1, user2);
    acceptFriendRequest(user2, user1);

    ResolveFriendRequestResponse expectedResponse = new ResolveFriendRequestResponse();
    expectedResponse
        .setError(new Error(ResolveFriendRequestResponse.ERROR_NO_FRIEND_REQUEST_EXIST));
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

    // Get user1 friends.

    List<Friendship> expectedUser1Friendships = new ArrayList<Friendship>();
    Friendship user1User5Friendship = Friendship.builder()
        .requesterId(user1.getId())
        .responderId(user5.getId())
        .isPending(true).build();
    expectedUser1Friendships.add(user1User5Friendship);

    Friendship user1User2Friendship = Friendship.builder()
        .requesterId(user1.getId())
        .responderId(user2.getId())
        .isPending(false).build();
    expectedUser1Friendships.add(user1User2Friendship);

    Friendship user1User3Friendship = Friendship.builder()
        .requesterId(user3.getId())
        .responderId(user1.getId())
        .isPending(false).build();
    expectedUser1Friendships.add(user1User3Friendship);

    authenticate(user1);
    MvcResult mvcResult =
        prepareGetRequest("/users/friends")
            .andExpect(status().isOk())
            .andReturn();

    GetFriendsResponse getUser1FriendsResponse =
        mapFromJson(mvcResult.getResponse().getContentAsString(), GetFriendsResponse.class);

    compareFriendshipList(getUser1FriendsResponse.getData(), expectedUser1Friendships);
  }

  @Test
  public void deleteFriend_normalScenario_shouldSucceed() throws Exception {
    makeFriends(user1, user2);
    assertThat(friendshipRepo.count(), is(1L));

    authenticate(user1);
    DeleteFriendResponse expectedResponse = new DeleteFriendResponse();
    prepareDeleteRequest(String.format("/users/friends/%s", user2.getId()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
    assertThat(friendshipRepo.count(), is(0L));
  }

  @Test
  public void deleteFriend_friendRequestIsPending_shouldRemoveFriendRequest() throws Exception {
    sendFriendRequest(user1, user2);
    assertThat(friendshipRepo.count(), is(1L));

    authenticate(user1);
    DeleteFriendResponse expectedResponse = new DeleteFriendResponse();
    prepareDeleteRequest(String.format("/users/friends/%s", user2.getId()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
    assertThat(friendshipRepo.count(), is(0L));
  }

  @Test
  public void deleteFriend_noFriendRequestExist_shouldNotSucceed() throws Exception {
    DeleteFriendResponse expectedResponse = new DeleteFriendResponse();
    expectedResponse.setError(new Error(DeleteFriendResponse.ERROR_NO_FRIENDSHIP));

    authenticate(user1);
    prepareDeleteRequest(String.format("/users/friends/%s", user2.getId()))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(mapToJson(expectedResponse)));
    assertThat(friendshipRepo.count(), is(0L));
  }

  private void compareFriendshipList(List<Friendship> actual, List<Friendship> expected) {
    Collections.sort(actual, Comparator.comparing(Friendship::getResponderId));
    Collections.sort(expected, Comparator.comparing(Friendship::getResponderId));

    assertThat(actual.size(), equalTo(expected.size()));

    for (int i = 0; i < 3; i++) {
      Friendship responseFriendship = actual.get(i);
      Friendship expectedFriendship = expected.get(i);
      assertThat(responseFriendship.getRequesterId(), equalTo(expectedFriendship.getRequesterId()));
      assertThat(responseFriendship.getResponderId(), equalTo(expectedFriendship.getResponderId()));
      assertThat(responseFriendship.isPending(), equalTo(expectedFriendship.isPending()));
    }
  }

  private ResultActions sendFriendRequest(User requester, User responder) throws Exception {
    authenticate(requester);
    return preparePostRequest(String.format("/users/friends/%s", responder.getId()),
        /*body=*/null);
  }

  private ResultActions acceptFriendRequest(User responder, User requester) throws Exception {
    ResolveFriendRequestBody body = new ResolveFriendRequestBody();
    body.setAccept(true);
    authenticate(responder);
    return preparePutRequest(String.format("/users/friends/%s", requester.getId()),
        mapToJson(body));
  }

  private ResultActions rejectFriendRequest(User responder, User requester) throws Exception {
    ResolveFriendRequestBody body = new ResolveFriendRequestBody();
    body.setAccept(false);
    authenticate(responder);
    return preparePutRequest(String.format("/users/friends/%s", requester.getId()),
        mapToJson(body));
  }

  private void makeFriends(User user1, User user2) throws Exception {
    sendFriendRequest(user1, user2);
    acceptFriendRequest(user2, user1);
  }
}
