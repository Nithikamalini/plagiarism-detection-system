package com.plagiarism.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Complete Plagiarism Analysis Report stored in MongoDB and returned to the UI.
 */
public class PlagiarismReport {
    private String id;
    private String userId;
    private String documentTitle;
    private String originalContent;
    private String highlightedSourceHtml;
    private String highlightedMatchedHtml;
    private double overallSimilarity; // 0.0 to 100.0%
    private String similarityLevel; // LOW, MEDIUM, HIGH
    private double cosineSimilarity; // 0.0 to 100.0%
    private double ngramSimilarity; // 0.0 to 100.0%
    private int totalWords;
    private int matchedWords;
    private String topMatchedDocId;
    private String topMatchedDocTitle;
    private List<MatchSegment> matchedSegments;
    private List<DocumentMatchSummary> documentBreakdown;
    private Date checkedAt;

    public static class DocumentMatchSummary {
        private String documentId;
        private String title;
        private double similarityScore; // 0 - 100%
        private double cosineScore;
        private double ngramScore;
        private int matchCount;

        public DocumentMatchSummary() {}

        public DocumentMatchSummary(String documentId, String title, double similarityScore, double cosineScore, double ngramScore, int matchCount) {
            this.documentId = documentId;
            this.title = title;
            this.similarityScore = similarityScore;
            this.cosineScore = cosineScore;
            this.ngramScore = ngramScore;
            this.matchCount = matchCount;
        }

        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public double getSimilarityScore() { return similarityScore; }
        public void setSimilarityScore(double similarityScore) { this.similarityScore = similarityScore; }

        public double getCosineScore() { return cosineScore; }
        public void setCosineScore(double cosineScore) { this.cosineScore = cosineScore; }

        public double getNgramScore() { return ngramScore; }
        public void setNgramScore(double ngramScore) { this.ngramScore = ngramScore; }

        public int getMatchCount() { return matchCount; }
        public void setMatchCount(int matchCount) { this.matchCount = matchCount; }
    }

    public PlagiarismReport() {
        this.checkedAt = new Date();
        this.matchedSegments = new ArrayList<>();
        this.documentBreakdown = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDocumentTitle() { return documentTitle; }
    public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }

    public String getOriginalContent() { return originalContent; }
    public void setOriginalContent(String originalContent) { this.originalContent = originalContent; }

    public String getHighlightedSourceHtml() { return highlightedSourceHtml; }
    public void setHighlightedSourceHtml(String highlightedSourceHtml) { this.highlightedSourceHtml = highlightedSourceHtml; }

    public String getHighlightedMatchedHtml() { return highlightedMatchedHtml; }
    public void setHighlightedMatchedHtml(String highlightedMatchedHtml) { this.highlightedMatchedHtml = highlightedMatchedHtml; }

    public double getOverallSimilarity() { return overallSimilarity; }
    public void setOverallSimilarity(double overallSimilarity) { this.overallSimilarity = overallSimilarity; }

    public String getSimilarityLevel() { return similarityLevel; }
    public void setSimilarityLevel(String similarityLevel) { this.similarityLevel = similarityLevel; }

    public double getCosineSimilarity() { return cosineSimilarity; }
    public void setCosineSimilarity(double cosineSimilarity) { this.cosineSimilarity = cosineSimilarity; }

    public double getNgramSimilarity() { return ngramSimilarity; }
    public void setNgramSimilarity(double ngramSimilarity) { this.ngramSimilarity = ngramSimilarity; }

    public int getTotalWords() { return totalWords; }
    public void setTotalWords(int totalWords) { this.totalWords = totalWords; }

    public int getMatchedWords() { return matchedWords; }
    public void setMatchedWords(int matchedWords) { this.matchedWords = matchedWords; }

    public String getTopMatchedDocId() { return topMatchedDocId; }
    public void setTopMatchedDocId(String topMatchedDocId) { this.topMatchedDocId = topMatchedDocId; }

    public String getTopMatchedDocTitle() { return topMatchedDocTitle; }
    public void setTopMatchedDocTitle(String topMatchedDocTitle) { this.topMatchedDocTitle = topMatchedDocTitle; }

    public List<MatchSegment> getMatchedSegments() { return matchedSegments; }
    public void setMatchedSegments(List<MatchSegment> matchedSegments) { this.matchedSegments = matchedSegments; }

    public List<DocumentMatchSummary> getDocumentBreakdown() { return documentBreakdown; }
    public void setDocumentBreakdown(List<DocumentMatchSummary> documentBreakdown) { this.documentBreakdown = documentBreakdown; }

    public Date getCheckedAt() { return checkedAt; }
    public void setCheckedAt(Date checkedAt) { this.checkedAt = checkedAt; }
}
