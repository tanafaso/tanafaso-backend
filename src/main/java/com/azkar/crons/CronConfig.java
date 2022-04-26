package com.azkar.crons;

import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

@Component
@Lazy(value = false)
@EnableScheduling
public class CronConfig {

}
