package com.plagiarism.algo;

import java.util.*;

/**
 * Calculates Cosine Similarity between two text documents using
 * Term Frequency (TF) Vector Space Model.
 */
public class CosineSimilarity {

    /**
     * Compute Cosine Similarity between text1 and text2.
     * Returns a score between 0.0 (completely distinct) and 1.0 (identical vector distribution).
     */
    public static double calculate(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }

        List<String> tokens1 = TextPreprocessor.removeStopWords(TextPreprocessor.tokenize(text1));
        List<String> tokens2 = TextPreprocessor.removeStopWords(TextPreprocessor.tokenize(text2));

        if (tokens1.isEmpty() && tokens2.isEmpty()) {
            return 1.0;
        }
        if (tokens1.isEmpty() || tokens2.isEmpty()) {
            return 0.0;
        }

        // Calculate Term Frequency maps
        Map<String, Integer> tf1 = getTermFrequencies(tokens1);
        Map<String, Integer> tf2 = getTermFrequencies(tokens2);

        // Build combined vocabulary
        Set<String> vocabulary = new HashSet<>();
        vocabulary.addAll(tf1.keySet());
        vocabulary.addAll(tf2.keySet());

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (String term : vocabulary) {
            int count1 = tf1.getOrDefault(term, 0);
            int count2 = tf2.getOrDefault(term, 0);

            dotProduct += (double) count1 * count2;
            norm1 += (double) count1 * count1;
            norm2 += (double) count2 * count2;
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        double similarity = dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
        return Math.min(1.0, Math.max(0.0, similarity));
    }

    private static Map<String, Integer> getTermFrequencies(List<String> tokens) {
        Map<String, Integer> freqs = new HashMap<>();
        for (String token : tokens) {
            freqs.put(token, freqs.getOrDefault(token, 0) + 1);
        }
        return freqs;
    }
}
