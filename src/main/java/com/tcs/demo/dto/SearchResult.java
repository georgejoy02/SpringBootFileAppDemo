package com.tcs.demo.dto;

public class SearchResult {
    private String fileName;
    private String key;
    
    public SearchResult() {}
    
    public SearchResult(String fileName, String key) {
        this.fileName = fileName;
        this.key = key;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
}
