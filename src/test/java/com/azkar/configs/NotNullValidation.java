package com.azkar.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.azkar.TestBase;
import com.azkar.entities.Group;
import com.azkar.repos.GroupRepo;
import javax.validation.ConstraintViolationException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class NotNullValidation extends TestBase {

  @Autowired
  GroupRepo groupRepo;

  @Test
  public void instantiation_notNullAnnotatedFieldKeptNull_shouldSucceed() {
    Group group = Group.builder().name(null).build();
    assertThat(group.getName(), nullValue());
  }

  @Test
  public void persistence_notNullAnnotatedFieldKeptNull_shouldThrowException() {
    Group group = Group.builder().name(null).build();
    assertThrows(ConstraintViolationException.class, () -> {
      groupRepo.save(group);
    });
  }
}
