package com.azkar.services;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import java.io.FileInputStream;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FCMService {

  @Value("${app.firebase-config-path}")
  String firebaseConfigPath;

  public void initialize() throws IOException {
    FileInputStream refreshToken = new FileInputStream(firebaseConfigPath);

    FirebaseOptions options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(refreshToken))
        .build();

    FirebaseApp.initializeApp(options);
  }

  public String sendMessage(String registrationToken) throws FirebaseMessagingException {
    Message message = Message.builder()
        .putData("key", "value")
        .setToken(registrationToken)
        .build();

    String response = FirebaseMessaging.getInstance().send(message);
    System.out.println("Response from firebase: " + response);
    return response;
  }
}
