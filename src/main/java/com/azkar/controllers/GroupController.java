package com.azkar.controllers;

import com.azkar.entities.Group;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.groupcontroller.AddGroupRequest;
import com.azkar.payload.groupcontroller.AddGroupResponse;
import com.azkar.repos.GroupRepo;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GroupController extends BaseController {

  @Autowired
  private GroupRepo groupRepo;

  @PostMapping(path = "/group", consumes = "application/json", produces = "application/json")
  public ResponseEntity<AddGroupResponse> addGroup(@RequestBody AddGroupRequest req) {
    AddGroupResponse response = new AddGroupResponse();
    String userId = currentUser.getId();
    if (Strings.isNullOrEmpty(req.getName())) {
      response.setError(new Error("Cannot create a group with empty name."));
      return ResponseEntity.badRequest().body(response);
    }
    Group newGroup =
        Group.builder()
            .name(req.getName())
            .adminId(userId)
            .cardinality(req.getCardinality())
            .usersIds(new ArrayList<>(Arrays.asList(userId)))
            .build();
    groupRepo.save(newGroup);
    response.setData(newGroup);
    return ResponseEntity.ok(response);
  }
}
