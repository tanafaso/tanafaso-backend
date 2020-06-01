package com.azkar.factories.controllers;

import com.azkar.entities.Group;
import java.util.ArrayList;
import java.util.List;

public class GroupFactory {

  static int groupsRequested = 0;

  public static Group getNewGroup(String adminId) {
    groupsRequested++;

    List<String> groupUsersIds = new ArrayList();
    groupUsersIds.add(adminId);

    Group group = Group.builder()
        .id("group_id" + groupsRequested)
        .adminId(adminId)
        .name("group_name" + groupsRequested)
        .usersIds(groupUsersIds)
        .build();

    return group;
  }
}
