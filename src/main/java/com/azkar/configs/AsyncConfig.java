package com.azkar.configs;

import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
  private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

  public static final String CONTROLLERS_TASK_EXECUTOR = "tanafaso-task-executor";

  @Bean(name = CONTROLLERS_TASK_EXECUTOR)
  public TaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(2);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix(CONTROLLERS_TASK_EXECUTOR);
    executor.initialize();

    logger.info("Async configs setup done");
    return executor;
  }
}
