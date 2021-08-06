package com.jai.plays.tweetcrawler;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "dev.tweet")
@Getter
@Setter
@ToString
public class TwitterConfig {
    private String consumerkey;
    private String consumersecret;
    private String token;
    private String secret;
}
