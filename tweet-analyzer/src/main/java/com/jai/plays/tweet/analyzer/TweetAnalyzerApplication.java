package com.jai.plays.tweet.analyzer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@SpringBootApplication
@Slf4j
public class TweetAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TweetAnalyzerApplication.class, args);
    }

    private SentimentAnalyzerService sentimentAnalyzerService;
    private MeterRegistry meterRegistry;
    private final Counter vNegCounter;
    private final Counter negCounter;
    private final Counter neuCounter;
    private final Counter posCounter;
    private final Counter vPosCounter;

    @Autowired
    public TweetAnalyzerApplication(SentimentAnalyzerService sentimentAnalyzerService,MeterRegistry meterRegistry) {
        this.sentimentAnalyzerService = sentimentAnalyzerService;
        vNegCounter = meterRegistry.counter(Rating.VERY_NEGATIVE.name());
        negCounter = meterRegistry.counter(Rating.NEGATIVE.name());
        neuCounter = meterRegistry.counter(Rating.NEUTRAL.name());
        posCounter = meterRegistry.counter(Rating.POSITIVE.name());
        vPosCounter = meterRegistry.counter(Rating.VERY_POSITIVE.name());
    }

    @Bean
    public Function<String, String> tweetAnalyzer() {
        return s -> {
            int rating = sentimentAnalyzerService.analyse(s);
            switch (rating){
                case 0: vNegCounter.increment();
                        break;
                case 1: negCounter.increment();
                        break;
                case 2: neuCounter.increment();
                        break;
                case 3: posCounter.increment();
                        break;
                case 4: vPosCounter.increment();
                        break;
            }
            log.info(s + " has rating of -> " + Rating.fromValue(rating));
            return s;
        };
    }
}
