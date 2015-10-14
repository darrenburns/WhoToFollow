package models;

import java.io.Serializable;

public class WordCount implements Serializable {

    public String word;
    public Integer count;

    public WordCount(String word, Integer count) {
        this.word = word;
        this.count = count;
    }

}
