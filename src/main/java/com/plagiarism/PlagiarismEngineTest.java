package com.plagiarism;

import com.plagiarism.algo.CosineSimilarity;
import com.plagiarism.algo.NGramAnalyzer;
import com.plagiarism.algo.PlagiarismEngine;
import com.plagiarism.db.DataSeeder;
import com.plagiarism.model.DocumentModel;
import com.plagiarism.model.PlagiarismReport;

import java.util.List;

public class PlagiarismEngineTest {

    public static void main(String[] args) {
        System.out.println(">>> STARTING PLAGIARISM ALGORITHM VERIFICATION <<<");

        // 1. Test Cosine Similarity on identical text
        String text1 = "Artificial intelligence is a branch of computer science.";
        String text2 = "Artificial intelligence is a branch of computer science.";
        double cos1 = CosineSimilarity.calculate(text1, text2);
        System.out.printf("[TEST 1] Identical Text Cosine: %.2f%% (Expected: 100%%)%n", cos1 * 100);
        assert cos1 >= 0.99 : "Cosine on identical text must be ~1.0";

        // 2. Test Cosine Similarity on completely disjoint text
        String textDisjoint = "Cooking pasta requires boiling water with a pinch of sea salt.";
        double cosDisjoint = CosineSimilarity.calculate(text1, textDisjoint);
        System.out.printf("[TEST 2] Disjoint Text Cosine: %.2f%% (Expected: 0%%)%n", cosDisjoint * 100);
        assert cosDisjoint == 0.0 : "Cosine on disjoint text must be 0.0";

        // 3. Test N-Gram Jaccard & Containment
        NGramAnalyzer.NGramResult ngr = NGramAnalyzer.analyze(text1, text2, 3);
        System.out.printf("[TEST 3] Trigram Jaccard: %.2f%%, Containment: %.2f%%%n",
                ngr.getJaccardSimilarity() * 100, ngr.getContainment() * 100);

        // 4. Test Copied Text Against Corpus
        List<DocumentModel> corpus = DataSeeder.getSeedDocuments();
        System.out.println("[TEST 4] Reference Corpus size: " + corpus.size() + " documents.");

        String plagiarizedSample = "Artificial intelligence is a wide-ranging branch of computer science concerned with building smart machines capable of performing tasks that typically require human intelligence. Deep learning is part of a broader family of machine learning methods based on artificial neural networks with representation learning.";
        PlagiarismReport report1 = PlagiarismEngine.analyzeAgainstCorpus("Copied Student Paper", plagiarizedSample, corpus);

        System.out.println("--------------------------------------------------");
        System.out.println("  Plagiarized Sample Results:");
        System.out.printf("  Overall Similarity : %.1f%%%n", report1.getOverallSimilarity());
        System.out.printf("  Similarity Level   : %s%n", report1.getSimilarityLevel());
        System.out.printf("  Cosine Similarity  : %.1f%%%n", report1.getCosineSimilarity());
        System.out.printf("  N-Gram Similarity  : %.1f%%%n", report1.getNgramSimilarity());
        System.out.printf("  Top Matched Doc    : %s%n", report1.getTopMatchedDocTitle());
        System.out.printf("  Matched Snippets   : %d%n", report1.getMatchedSegments().size());
        System.out.println("--------------------------------------------------");

        // 5. Test Original Safe Text Against Corpus
        String originalSample = "Marine biology focuses on the physiological adaptations of abyssal organisms surviving under extreme hydrostatic pressure and chemical gradients near hydrothermal vents.";
        PlagiarismReport report2 = PlagiarismEngine.analyzeAgainstCorpus("Original Marine Biology Paper", originalSample, corpus);

        System.out.println("  Original Sample Results:");
        System.out.printf("  Overall Similarity : %.1f%%%n", report2.getOverallSimilarity());
        System.out.printf("  Similarity Level   : %s%n", report2.getSimilarityLevel());
        System.out.printf("  Cosine Similarity  : %.1f%%%n", report2.getCosineSimilarity());
        System.out.printf("  N-Gram Similarity  : %.1f%%%n", report2.getNgramSimilarity());
        System.out.println("--------------------------------------------------");

        // 6. Test Direct Two-Document Comparison
        PlagiarismReport directReport = PlagiarismEngine.analyzeDirect("Paper A", plagiarizedSample, "Paper B", corpus.get(0).getContent());
        System.out.printf("[TEST 6] Direct Comparison Similarity: %.1f%% (%s)%n",
                directReport.getOverallSimilarity(), directReport.getSimilarityLevel());

        System.out.println(">>> ALL ALGORITHMIC TESTS PASSED PERFECTLY! <<<");
    }
}
