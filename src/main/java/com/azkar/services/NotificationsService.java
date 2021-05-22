package com.azkar.services;

import com.azkar.entities.User;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import java.io.IOException;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationsService {

  private static final Logger logger = LoggerFactory.getLogger(NotificationsService.class);

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

  public void sendNotificationToUser(User user, String title, String body) {
    if (user.getNotificationsToken() == null || user.getNotificationsToken().isEmpty()) {
      logger.error(String.format("Token not found for user: %s", user.getId()));
      return;
    }
    Message message = Message.builder()
                             .setToken(user.getNotificationsToken())
                             .setNotification(
                                 Notification.builder().setTitle(title).setBody(body).build())
                             .build();

    try {
      FirebaseMessaging.getInstance().send(message);
    } catch (FirebaseMessagingException e) {
      logger.error(
          String.format("Failed to send a notification to user: %s with title: %s, and body: %s",
              user.getId(), title, body), e);
    }
  }
}
