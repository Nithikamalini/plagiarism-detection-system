package com.plagiarism.algo;

import java.util.*;

/**
 * N-Gram Analysis for detecting phrase-level copying and syntactical overlap.
 */
public class NGramAnalyzer {

    public static class NGramResult {
        private final double jaccardSimilarity;
        private final double containment;
        private final Set<String> matchingNGrams;
        private final int totalNGrams1;
        private final int totalNGrams2;

        public NGramResult(double jaccardSimilarity, double containment, Set<String> matchingNGrams, int totalNGrams1, int totalNGrams2) {
            this.jaccardSimilarity = jaccardSimilarity;
            this.containment = containment;
            this.matchingNGrams = matchingNGrams;
            this.totalNGrams1 = totalNGrams1;
            this.totalNGrams2 = totalNGrams2;
        }

        public double getJaccardSimilarity() { return jaccardSimilarity; }
        public double getContainment() { return containment; }
        public Set<String> getMatchingNGrams() { return matchingNGrams; }
        public int getTotalNGrams1() { return totalNGrams1; }
        public int getTotalNGrams2() { return totalNGrams2; }
    }

    /**
     * Perform N-Gram analysis between text1 (submitted document) and text2 (reference document).
     * @param text1 Input text to test
     * @param text2 Reference text
     * @param n Size of N-Gram (typically 3 for trigrams)
     */
    public static NGramResult analyze(String text1, String text2, int n) {
        if (text1 == null || text2 == null) {
            return new NGramResult(0.0, 0.0, Collections.emptySet(), 0, 0);
        }

        List<String> tokens1 = TextPreprocessor.tokenize(text1);
        List<String> tokens2 = TextPreprocessor.tokenize(text2);

        List<String> ngrams1 = TextPreprocessor.generateWordNGrams(tokens1, n);
        List<String> ngrams2 = TextPreprocessor.generateWordNGrams(tokens2, n);

        Set<String> set1 = new HashSet<>(ngrams1);
        Set<String> set2 = new HashSet<>(ngrams2);

        if (set1.isEmpty() && set2.isEmpty()) {
            return new NGramResult(1.0, 1.0, Collections.emptySet(), 0, 0);
        }
        if (set1.isEmpty() || set2.isEmpty()) {
            return new NGramResult(0.0, 0.0, Collections.emptySet(), set1.size(), set2.size());
        }

        // Intersection (matching n-grams)
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        // Union
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        double jaccard = (double) intersection.size() / union.size();
        // Containment: how much of the tested document (set1) is present in set2
        double containment = (double) intersection.size() / set1.size();

        return new NGramResult(jaccard, containment, intersection, set1.size(), set2.size());
    }
}
