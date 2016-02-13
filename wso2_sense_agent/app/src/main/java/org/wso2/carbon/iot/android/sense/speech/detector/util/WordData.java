package org.wso2.carbon.iot.android.sense.speech.detector.util;

/**
 * This defines the data structure of the word data.
 */
public class WordData {
    /**
     * timestamp for all the occurences
     */
    private String timestamps;
    private int occurences;
    private String word;
    private String sessionId;

    public WordData(String sessionId, String word, int occurences, String timestamps) {
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

    /**
     * @param occurences for the word and then add the timestamp for each occurences.
     */
    public void addOccurences(int occurences) {
        this.occurences = this.occurences + occurences;
        if (occurences > 0) {
            if (timestamps == null) {
                timestamps = "";
            }
            Long tsLong = System.currentTimeMillis() / 1000;
            String currentTimestamp = tsLong.toString();
            for (int i = 0; i < occurences; i++) {
                if (timestamps.isEmpty()) {
                    timestamps = currentTimestamp;
                    continue;
                }
                timestamps = timestamps + "," + currentTimestamp;
            }
        }
    }


}
