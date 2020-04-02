package com.omar.azkar.controllers;

import com.omar.azkar.entities.Group;
import com.omar.azkar.repos.GroupRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthorizationCodeAuthenticationToken;
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
        Group newGroup = new Group();
        newGroup.setName(req.getName());
        newGroup.setAdminId("5e837b4babae1b83f1b636b0");
        newGroup.setBinary(req.getIsBinary());
        newGroup.setChallenges(new ArrayList<>());
        List<String> usersIds = new ArrayList<>();
        usersIds.add("5e837b4babae1b83f1b636b0");
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
