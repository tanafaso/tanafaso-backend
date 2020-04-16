package com.azkar.factories;

import com.azkar.entities.Group;
import com.azkar.entities.Group.GroupCardinality;
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
        .cardinality(GroupCardinality.MULTI)
        .usersIds(groupUsersIds)
        .build();

    return group;
  }
}
