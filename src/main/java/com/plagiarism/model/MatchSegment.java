package com.plagiarism.model;

/**
 * Represents a matched segment/passage between submitted text and reference document.
 */
public class MatchSegment {
    private String sourceSnippet;
    private String matchedSnippet;
    private String referenceDocId;
    private String referenceDocTitle;
    private double similarityScore; // 0.0 - 1.0
    private String matchType; // EXACT, HIGH, MODERATE

    public MatchSegment() {}

    public MatchSegment(String sourceSnippet, String matchedSnippet, String referenceDocId, String referenceDocTitle, double similarityScore, String matchType) {
        this.sourceSnippet = sourceSnippet;
        this.matchedSnippet = matchedSnippet;
        this.referenceDocId = referenceDocId;
        this.referenceDocTitle = referenceDocTitle;
        this.similarityScore = similarityScore;
        this.matchType = matchType;
    }

    public String getSourceSnippet() { return sourceSnippet; }
    public void setSourceSnippet(String sourceSnippet) { this.sourceSnippet = sourceSnippet; }

    public String getMatchedSnippet() { return matchedSnippet; }
    public void setMatchedSnippet(String matchedSnippet) { this.matchedSnippet = matchedSnippet; }

    public String getReferenceDocId() { return referenceDocId; }
    public void setReferenceDocId(String referenceDocId) { this.referenceDocId = referenceDocId; }

    public String getReferenceDocTitle() { return referenceDocTitle; }
    public void setReferenceDocTitle(String referenceDocTitle) { this.referenceDocTitle = referenceDocTitle; }

    public double getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(double similarityScore) { this.similarityScore = similarityScore; }

    public String getMatchType() { return matchType; }
    public void setMatchType(String matchType) { this.matchType = matchType; }
}
