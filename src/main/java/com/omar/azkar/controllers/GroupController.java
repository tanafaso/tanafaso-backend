package com.omar.azkar.controllers;

import com.omar.azkar.configs.jwt.UserPrincipal;
import com.omar.azkar.entities.Group;
import com.omar.azkar.repos.GroupRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;


@RestController
public class GroupController {

  @Autowired
  private GroupRepo groupRepo;

  @PostMapping(path = "/group", consumes = "application/json", produces = "application/json")
  public Group addGroup(@RequestBody AddGroupRequest req) {
    UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder
        .getContext()
        .getAuthentication()
        .getPrincipal();
    String userId = userPrincipal.user.getId();
    Group newGroup = new Group();
    newGroup.setName(req.getName());
    newGroup.setAdminId(userId);
    newGroup.setBinary(req.getIsBinary());
    newGroup.setChallenges(new ArrayList<>());
    List<String> usersIds = new ArrayList<>();
    usersIds.add(userId);
    newGroup.setUsersIds(usersIds);
    groupRepo.save(newGroup);
    return newGroup;
  }

  private static class AddGroupRequest {

    String name;
    boolean isBinary;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public boolean getIsBinary() {
      return isBinary;
    }

    public void setIsBinary(boolean binary) {
      this.isBinary = binary;
    }
  }
}
