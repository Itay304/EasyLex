package com.example.easylex.data;

public class WordList {
    private String id;
    private String name;
    private int wordCount;

    public WordList() {}

    public WordList(String id, String name, int wordCount) {
        this.id = id;
        this.name = name;
        this.wordCount = wordCount;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getWordCount() { return wordCount; }
}