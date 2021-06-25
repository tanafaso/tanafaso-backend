package com.azkar.configs;

import com.github.mongobee.Mongobee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Configuration
@Service
public class MongobeeConfig {

  @Autowired
  MongoTemplate mongoTemplate;
  @Value("${spring.data.mongodb.uri}")
  private String dbUri;
  @Value("${spring.data.mongodb.name}")
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
