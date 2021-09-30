package com.azkar.services;

import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.repos.GroupRepo;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GroupsService {

  @Autowired
  GroupRepo groupRepo;

  public List<Group> getGroups(User user) {
    return groupRepo.findAll().stream()
        .filter(group -> group.getUsersIds().contains(user.getId()))
        .collect(Collectors.toList());
  }
}
