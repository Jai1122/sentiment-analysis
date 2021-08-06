package com.jai.plays.tweet.analyzer;

public enum Rating {
    VERY_NEGATIVE(0), NEGATIVE(1), NEUTRAL(2), POSITIVE(3), VERY_POSITIVE(4);

    int value;

    private Rating(int value) {
        this.value = value;
    }

    public static Rating fromValue(int value) {
        for (Rating typeSentiment : values()) {
            if (typeSentiment.value == value) {
                return typeSentiment;
            }
        }
        return Rating.NEUTRAL;
    }
}
