package com.familyfinance.budget.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableKafka
@ConditionalOnProperty(name = "familyfinance.kafka.enabled", havingValue = "true")
public class KafkaConfig {
}
