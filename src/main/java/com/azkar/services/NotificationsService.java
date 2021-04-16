package com.azkar.services;

import com.azkar.entities.User;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import java.io.IOException;
import javax.annotation.PostConstruct;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationsService {

  private static final Logger logger = LoggerFactory.getLogger(NotificationsService.class);

  private static final String TITLE_MAP_KEY = "title";
  private static final String BODY_MAP_KEY = "body";

  @PostConstruct
  public void initialize() {
    FirebaseOptions options = null;
    try {
      options = FirebaseOptions.builder()
          .setCredentials(GoogleCredentials.getApplicationDefault())
          .build();
      FirebaseApp.initializeApp(options);
    } catch (IOException e) {
      logger.error("Could not initialize Firebase app correctly.", e);
    }
  }

  public String sendNotificationToUser(Notification notification, User user) {
    Message message = Message.builder()
        .putData(TITLE_MAP_KEY, notification.getTitle())
        .putData(BODY_MAP_KEY, notification.getBody())
        .setToken(user.getNotificationsToken())
        .build();

    String response = null;
    try {
      response = FirebaseMessaging.getInstance().send(message);
    } catch (FirebaseMessagingException e) {
      logger.error(String.format("Failed to send a notification with title: %s, and body: %s",
          notification.title, notification.body), e);
    }
    return response;
  }

  @Builder
  @Getter
  public static class Notification {

    String title;
    String body;
  }
}
