package com.jai.plays.tweetcrawler;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "dev.kafka")
@Getter
@Setter
@ToString
public class KafkaConfig {
    private String bootstrapServer;
    private String topic;
}
