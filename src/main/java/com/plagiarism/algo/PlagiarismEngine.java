package com.plagiarism.algo;

import com.plagiarism.model.DocumentModel;
import com.plagiarism.model.MatchSegment;
import com.plagiarism.model.PlagiarismReport;

import java.util.*;

/**
 * Core Plagiarism Detection Engine.
 * Integrates Cosine Similarity, N-Gram Analysis, Sentence Matching, and Segment Highlighting.
 */
public class PlagiarismEngine {

    private static final int DEFAULT_NGRAM_SIZE = 3;

    /**
     * Compare input text against a collection of reference documents.
     */
    public static PlagiarismReport analyzeAgainstCorpus(String inputTitle, String inputText, List<DocumentModel> corpus) {
        PlagiarismReport report = new PlagiarismReport();
        report.setId("REP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        report.setDocumentTitle(inputTitle != null && !inputTitle.trim().isEmpty() ? inputTitle : "Untitled Document");
        report.setOriginalContent(inputText);

        int totalWords = countWords(inputText);
        report.setTotalWords(totalWords);

        if (inputText == null || inputText.trim().isEmpty() || corpus == null || corpus.isEmpty()) {
            report.setOverallSimilarity(0.0);
            report.setSimilarityLevel("LOW");
            report.setCosineSimilarity(0.0);
            report.setNgramSimilarity(0.0);
            report.setHighlightedSourceHtml(escapeHtml(inputText != null ? inputText : ""));
            report.setHighlightedMatchedHtml("<em>No reference documents available for comparison.</em>");
            return report;
        }

        double maxSimilarity = 0.0;
        double maxCosine = 0.0;
        double maxNgram = 0.0;
        DocumentModel topDoc = null;
        List<MatchSegment> allSegments = new ArrayList<>();
        List<PlagiarismReport.DocumentMatchSummary> breakdown = new ArrayList<>();

        // Analyze against each document in corpus
        for (DocumentModel doc : corpus) {
            if (doc.getContent() == null || doc.getContent().trim().isEmpty()) {
                continue;
            }

            double cosine = CosineSimilarity.calculate(inputText, doc.getContent()) * 100.0;
            NGramAnalyzer.NGramResult ngram = NGramAnalyzer.analyze(inputText, doc.getContent(), DEFAULT_NGRAM_SIZE);
            double ngramScore = ngram.getContainment() * 100.0;

            // Combined similarity for this reference doc (50% Cosine + 50% N-Gram Containment)
            double docCombined = (cosine * 0.45) + (ngramScore * 0.55);
            docCombined = Math.min(100.0, Math.max(0.0, docCombined));

            // Find matching sentence snippets
            List<MatchSegment> docSegments = findMatchingSegments(inputText, doc.getContent(), doc.getId(), doc.getTitle());

            breakdown.add(new PlagiarismReport.DocumentMatchSummary(
                    doc.getId(),
                    doc.getTitle(),
                    round(docCombined),
                    round(cosine),
                    round(ngramScore),
                    docSegments.size()
            ));

            if (docCombined > maxSimilarity) {
                maxSimilarity = docCombined;
                maxCosine = cosine;
                maxNgram = ngramScore;
                topDoc = doc;
            }

            allSegments.addAll(docSegments);
        }

        // Sort breakdown by highest similarity
        breakdown.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));

        // Cap overall similarity
        double overallScore = round(maxSimilarity);
        report.setOverallSimilarity(overallScore);
        report.setCosineSimilarity(round(maxCosine));
        report.setNgramSimilarity(round(maxNgram));
        report.setDocumentBreakdown(breakdown);

        // Determine Level
        if (overallScore < 20.0) {
            report.setSimilarityLevel("LOW");
        } else if (overallScore <= 50.0) {
            report.setSimilarityLevel("MEDIUM");
        } else {
            report.setSimilarityLevel("HIGH");
        }

        if (topDoc != null) {
            report.setTopMatchedDocId(topDoc.getId());
            report.setTopMatchedDocTitle(topDoc.getTitle());
            List<MatchSegment> topSegments = findMatchingSegments(inputText, topDoc.getContent(), topDoc.getId(), topDoc.getTitle());
            report.setMatchedSegments(topSegments);
            report.setHighlightedSourceHtml(generateHighlightedHtml(inputText, topSegments, true));
            report.setHighlightedMatchedHtml(generateHighlightedHtml(topDoc.getContent(), topSegments, false));
        } else {
            report.setHighlightedSourceHtml(escapeHtml(inputText));
            report.setHighlightedMatchedHtml("<em>No matching content found.</em>");
        }

        // Matched words count estimate
        int matchedWords = 0;
        for (MatchSegment seg : report.getMatchedSegments()) {
            matchedWords += countWords(seg.getSourceSnippet());
        }
        report.setMatchedWords(Math.min(totalWords, matchedWords));

        return report;
    }

    /**
     * Direct comparison between Document 1 and Document 2.
     */
    public static PlagiarismReport analyzeDirect(String title1, String text1, String title2, String text2) {
        DocumentModel refDoc = new DocumentModel("REF-1", "SYSTEM", title2, "doc2.txt", text2, "Direct");
        return analyzeAgainstCorpus(title1, text1, Collections.singletonList(refDoc));
    }

    /**
     * Identify matching sentence and phrase segments between source text and reference text.
     */
    private static List<MatchSegment> findMatchingSegments(String source, String reference, String refDocId, String refDocTitle) {
        List<MatchSegment> segments = new ArrayList<>();
        List<String> sourceSentences = TextPreprocessor.splitSentences(source);
        List<String> refSentences = TextPreprocessor.splitSentences(reference);

        for (String srcSent : sourceSentences) {
            double bestSentSim = 0.0;
            String bestRefSent = null;

            for (String refSent : refSentences) {
                double cos = CosineSimilarity.calculate(srcSent, refSent);
                NGramAnalyzer.NGramResult ngr = NGramAnalyzer.analyze(srcSent, refSent, 2);
                double combined = (cos * 0.5) + (ngr.getJaccardSimilarity() * 0.5);

                if (combined > bestSentSim) {
                    bestSentSim = combined;
                    bestRefSent = refSent;
                }
            }

            // If match exceeds similarity threshold
            if (bestSentSim >= 0.45 && bestRefSent != null) {
                String type = bestSentSim >= 0.80 ? "EXACT" : (bestSentSim >= 0.60 ? "HIGH" : "MODERATE");
                segments.add(new MatchSegment(
                        srcSent,
                        bestRefSent,
                        refDocId,
                        refDocTitle,
                        round(bestSentSim * 100.0),
                        type
                ));
            }
        }

        return segments;
    }

    /**
     * Generates HTML with highlighted tags for matched passages.
     */
    private static String generateHighlightedHtml(String text, List<MatchSegment> segments, boolean isSource) {
        if (text == null) return "";
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        String escaped = escapeHtml(normalized);

        for (MatchSegment seg : segments) {
            String target = isSource ? seg.getSourceSnippet() : seg.getMatchedSnippet();
            if (target == null || target.trim().isEmpty()) continue;

            String escapedTarget = escapeHtml(target.trim());
            String colorClass = "match-moderate";
            if ("EXACT".equalsIgnoreCase(seg.getMatchType())) {
                colorClass = "match-exact";
            } else if ("HIGH".equalsIgnoreCase(seg.getMatchType())) {
                colorClass = "match-high";
            }

            String highlightTag = "<mark class=\"plag-highlight " + colorClass + "\" title=\"Similarity: "
                    + seg.getSimilarityScore() + "% (" + seg.getMatchType() + ")\">"
                    + escapedTarget + "</mark>";

            // Replace first occurrence
            escaped = escaped.replace(escapedTarget, highlightTag);
        }

        // Preserve line breaks
        return escaped.replace("\n", "<br/>");
    }

    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static int countWords(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        return text.trim().split("\\s+").length;
    }

    private static double round(double val) {
        return Math.round(val * 10.0) / 10.0;
    }
}
