package org.wso2.carbon.iot.android.sense.speech.detector.util;

/**
 * This defines the datastructur of the word data.
 */
public class WordData {
    /**
     * timestamp for all the occurences
     */
    private String timestamps;
    private int occurences;
    private String word;
    private String sessionId;

    public WordData(String sessionId, String word, int occurences, String timestamps){
        this.timestamps = timestamps;
        this.occurences = occurences;
        this.word = word;
        this.sessionId = sessionId;
    }

    public String getTimestamps() {
        return timestamps;
    }

    public int getOccurences() {
        return occurences;
    }

    public String getWord() {
        return word;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void addOccurences(int occurences) {
        this.occurences = this.occurences + occurences;
    }
}
