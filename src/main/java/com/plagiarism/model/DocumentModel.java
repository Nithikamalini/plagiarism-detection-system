package com.plagiarism.model;

import java.util.Date;

/**
 * Document entity stored in MongoDB corpus for plagiarism comparison.
 */
public class DocumentModel {
    private String id;
    private String userId;
    private String title;
    private String fileName;
    private String content;
    private int wordCount;
    private String category;
    private Date uploadedAt;

    public DocumentModel() {
        this.uploadedAt = new Date();
    }

    public DocumentModel(String id, String userId, String title, String fileName, String content, String category) {
        this();
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.fileName = fileName;
        this.content = content;
        this.category = category != null ? category : "General";
        this.wordCount = calculateWords(content);
    }

    private int calculateWords(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        return text.trim().split("\\s+").length;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getContent() { return content; }
    public void setContent(String content) {
        this.content = content;
        this.wordCount = calculateWords(content);
    }

    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Date getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Date uploadedAt) { this.uploadedAt = uploadedAt; }
}
