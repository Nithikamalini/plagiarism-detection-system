package com.plagiarism.algo;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Preprocessing utilities for textual analysis:
 * - Tokenization
 * - Stop words removal
 * - Punctuation stripping & lowercasing
 * - Sentence splitting
 * - N-gram generation
 */
public class TextPreprocessor {

    // Comprehensive English Stop Words Set
    public static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "aren't",
            "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by", "can",
            "can't", "cannot", "could", "couldn't", "did", "didn't", "do", "does", "doesn't", "doing", "don't",
            "down", "during", "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't", "have",
            "haven't", "having", "he", "he'd", "he'll", "he's", "her", "here", "here's", "hers", "herself", "him",
            "himself", "his", "how", "how's", "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't",
            "it", "it's", "its", "itself", "let's", "me", "more", "most", "mustn't", "my", "myself", "no", "nor",
            "not", "of", "off", "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves", "out",
            "over", "own", "same", "shan't", "she", "she'd", "she'll", "she's", "should", "shouldn't", "so", "some",
            "such", "than", "that", "that's", "the", "their", "theirs", "them", "themselves", "then", "there",
            "there's", "these", "they", "they'd", "they'll", "they're", "they've", "this", "those", "through", "to",
            "too", "under", "until", "up", "very", "was", "wasn't", "we", "we'd", "we'll", "we're", "we've", "were",
            "weren't", "what", "what's", "when", "when's", "where", "where's", "which", "while", "who", "who's",
            "whom", "why", "why's", "with", "won't", "would", "wouldn't", "you", "you'd", "you'll", "you're",
            "you've", "your", "yours", "yourself", "yourselves"
    ));

    /**
     * Clean and tokenize text into words, removing punctuation and converting to lowercase.
     */
    public static List<String> tokenize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> tokens = new ArrayList<>();
        // Match word sequences (letters and numbers)
        Matcher matcher = Pattern.compile("[a-zA-Z0-9]+").matcher(text.toLowerCase());
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    /**
     * Filter out standard English stop words from a token list.
     */
    public static List<String> removeStopWords(List<String> tokens) {
        List<String> filtered = new ArrayList<>();
        for (String t : tokens) {
            if (!STOP_WORDS.contains(t) && t.length() > 1) {
                filtered.add(t);
            }
        }
        return filtered;
    }

    /**
     * Split text into clean sentences.
     */
    public static List<String> splitSentences(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        String[] parts = normalized.split("(?<=[.!?])\\s+");
        List<String> sentences = new ArrayList<>();
        for (String p : parts) {
            String clean = p.trim();
            if (clean.length() > 5) {
                sentences.add(clean);
            }
        }
        return sentences;
    }

    /**
     * Generate word-level N-Grams from a list of tokens.
     */
    public static List<String> generateWordNGrams(List<String> tokens, int n) {
        List<String> ngrams = new ArrayList<>();
        if (tokens == null || tokens.size() < n) {
            if (tokens != null && !tokens.isEmpty()) {
                ngrams.add(String.join(" ", tokens));
            }
            return ngrams;
        }
        for (int i = 0; i <= tokens.size() - n; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < n; j++) {
                if (j > 0) sb.append(" ");
                sb.append(tokens.get(i + j));
            }
            ngrams.add(sb.toString());
        }
        return ngrams;
    }

    /**
     * Generate character-level N-Grams from raw text.
     */
    public static List<String> generateCharNGrams(String text, int n) {
        List<String> ngrams = new ArrayList<>();
        String normalized = text.replaceAll("\\s+", " ").trim().toLowerCase();
        if (normalized.length() < n) {
            ngrams.add(normalized);
            return ngrams;
        }
        for (int i = 0; i <= normalized.length() - n; i++) {
            ngrams.add(normalized.substring(i, i + n));
        }
        return ngrams;
    }
}
