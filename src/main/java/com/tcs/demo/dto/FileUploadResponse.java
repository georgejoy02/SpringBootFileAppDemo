package com.tcs.demo.dto;

public class FileUploadResponse {
    private Long id;
    private String originalFileName;
    private String changedFileName;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getOriginalFileName() {
        return originalFileName;
    }
    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }
    public String getChangedFileName() {
        return changedFileName;
    }
    public void setChangedFileName(String changedFileName) {
        this.changedFileName = changedFileName;
    }
}
