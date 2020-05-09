package com.azkar.configs;

import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {

  @Value("${mail.host}")
  private String mailHost;

  @Value("${mail.port}")
  private int mailPort;

  @Value("${mail.username}")
  private String mailUsername;

  @Value("${mail.password}")
  private String mailPassword;

  @Bean
  public JavaMailSender javaMailService() {
    JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();

    javaMailSender.setHost(mailHost);
    javaMailSender.setPort(mailPort);
    javaMailSender.setUsername(mailUsername);
    javaMailSender.setPassword(mailPassword);

    javaMailSender.setJavaMailProperties(getMailProperties());

    return javaMailSender;
  }

  private Properties getMailProperties() {
    Properties properties = new Properties();
    properties.setProperty("mail.transport.protocol", "smtp");
    properties.setProperty("mail.smtp.starttls.enable", "true");
    return properties;
  }
}
