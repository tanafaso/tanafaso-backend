package com.azkar;

import com.azkar.crons.ChallengesCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AzkarApplication {

  public static void main(String[] args) {
    SpringApplication.run(AzkarApplication.class, args);
  }
}
