package com.azkar.controllers;

import com.azkar.configs.authentication.UserPrincipal;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(consumes = "application/json", produces = "application/json")
public class GroupController extends BaseController {

  @Autowired
  private GroupRepo groupRepo;

  @PostMapping(path = "/group")
  public ResponseEntity<AddGroupResponse> addGroup(@RequestBody AddGroupRequest req) {
    AddGroupResponse response = new AddGroupResponse();
    UserPrincipal userPrincipal =
        (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    String userId = userPrincipal.getUserId();
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
