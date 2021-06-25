package com.azkar.configs;

import com.azkar.entities.Challenge;
import com.azkar.entities.Friendship;
import com.azkar.entities.Friendship.Friend;
import com.azkar.entities.Group;
import com.azkar.entities.User;
import com.azkar.entities.User.UserGroup;
import com.azkar.payload.utils.UserScore;
import com.github.mongobee.Mongobee;
import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Service;

@Configuration
@EnableMongoRepositories(basePackages = {"com.azkar.repos"})
@Service
public class MongobeeConfig {

  @Autowired
  MongoTemplate mongoTemplate;
  @Value("${spring.data.mongodb.uri}")
  private String dbUri;
  @Value("${DATABASE_NAME}")
  private String dbName;

  @Bean
  @Lazy(value = false)
  public Mongobee mongobee() {
    Mongobee runner = new Mongobee(dbUri);
    runner.setDbName(dbName);         // host must be set if not set in URI
    runner.setChangeLogsScanPackage(
        "com.azkar.configs"); // the package to be scanned for changesets
    runner.setMongoTemplate(mongoTemplate);
    return runner;
  }
}
