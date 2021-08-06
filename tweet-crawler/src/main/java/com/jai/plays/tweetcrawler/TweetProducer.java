package com.jai.plays.tweetcrawler;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.jai.plays.tweetcrawler.model.Tweet;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
@EnableAsync
@Slf4j
public class TweetProducer {

    private final TwitterConfig twitterConfig;
    private final KafkaConfig kafkaConfig;
    Gson gson = new Gson();

    @Value("${application.terms}")
    private String[] keywords;

    @Autowired
    public TweetProducer(TwitterConfig twitterConfig, KafkaConfig kafkaConfig) {
        this.twitterConfig = twitterConfig;
        this.kafkaConfig = kafkaConfig;
    }

    @Async
    @Scheduled(fixedRate = 60000)
    public void feedKafka() {
        BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(1000);
        String message = "";
        Client client = null;
        try {
            client = getTwitterClient(msgQueue);
            client.connect();
            message = msgQueue.poll(5, TimeUnit.SECONDS);
            if (message != null && !message.isEmpty()) {
                Tweet tweet = gson.fromJson(message, Tweet.class);
                if (tweet.getText() != null) {
                    log.info("Tweet text is " + tweet.getText());
                    writeToKafka(tweet.getText());
                } else {
                    log.error("Invalid tweet " + message);
                }
            }

        } catch (InterruptedException e) {
            log.error("InterruptedException occurred");
            e.printStackTrace();
        } catch (Exception e) {
            log.error("Exception occurred");
            e.printStackTrace();
        } finally {
            if (client != null) {
                client.stop();
            }
        }
    }


    public Client getTwitterClient(BlockingQueue<String> msgQueue) {
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
        List<String> terms = Lists.newArrayList(keywords);
        hosebirdEndpoint.trackTerms(terms);
        Authentication hosebirdAuth = new OAuth1(twitterConfig.getConsumerkey(),
                twitterConfig.getConsumersecret(), twitterConfig.getToken(), twitterConfig.getSecret());

        return new ClientBuilder()
                .name("Hosebird-Client-01")
                .hosts(hosebirdHosts)
                .authentication(hosebirdAuth)
                .endpoint(hosebirdEndpoint)
                .processor(new StringDelimitedProcessor(msgQueue)).build();
    }


    public void writeToKafka(String tweet) {
        String cleanedTweet   =   cleanTweet(tweet);
        KafkaProducer<String, String> producer = getKafkaProducer();
        ProducerRecord<String, String> record = new ProducerRecord<>(kafkaConfig.getTopic(), cleanedTweet);
        producer.send(record, new Callback() {
            @Override
            public void onCompletion(RecordMetadata metadata, Exception exception) {
                if (exception == null) {
                    log.info("Message published to topic :" + metadata.topic() + "\n" +
                            "Partition : " + metadata.partition() + "\n" +
                            "Offset : " + metadata.offset()
                    );
                } else {
                    log.error(exception.getLocalizedMessage());
                    exception.printStackTrace();
                }
            }
        });
    }

    public KafkaProducer<String, String> getKafkaProducer() {
        Properties producerProperties = new Properties();
        producerProperties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServer());
        producerProperties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProperties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProperties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");

        //create producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(producerProperties);
        return producer;

    }

    private String cleanTweet(String tweetText){
        // Clean up tweets
        String text = tweetText.trim()
                // remove links
                .replaceAll("http.*?[\\S]+", "")
                // remove usernames
                .replaceAll("@[\\S]+", "")
                // replace hashtags by just words
                .replaceAll("#", "")
                // correct all multiple white spaces to a single white space
                .replaceAll("[\\s]+", " ");
        return text;
    }
}
