package com.jai.plays.tweetcrawler.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Tweet {
    private String text;
    private User user;

}
