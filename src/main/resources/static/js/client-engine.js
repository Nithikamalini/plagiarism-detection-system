/**
 * Client-Side Plagiarism Detection Engine
 * Matches backend Java algorithms (Vector Space Cosine Similarity & N-Gram Containment)
 * Enables 100% Static Deployment (GitHub Pages, Hugging Face Static, Vercel, Netlify)
 */
const PlagiarismClientEngine = {
  STOP_WORDS: new Set([
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
  ]),

  tokenize(text) {
    if (!text) return [];
    const matches = text.toLowerCase().match(/[a-zA-Z0-9]+/g);
    return matches || [];
  },

  removeStopWords(tokens) {
    return tokens.filter(t => !this.STOP_WORDS.has(t) && t.length > 1);
  },

  splitSentences(text) {
    if (!text) return [];
    const normalized = text.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
    return normalized.split(/(?<=[.!?])\s+/).filter(s => s.trim().length > 5);
  },

  generateNGrams(tokens, n) {
    const ngrams = [];
    if (!tokens || tokens.length < n) {
      if (tokens && tokens.length > 0) ngrams.push(tokens.join(' '));
      return ngrams;
    }
    for (let i = 0; i <= tokens.length - n; i++) {
      ngrams.push(tokens.slice(i, i + n).join(' '));
    }
    return ngrams;
  },

  cosineSimilarity(text1, text2) {
    const tokens1 = this.removeStopWords(this.tokenize(text1));
    const tokens2 = this.removeStopWords(this.tokenize(text2));

    if (tokens1.length === 0 && tokens2.length === 0) return 1.0;
    if (tokens1.length === 0 || tokens2.length === 0) return 0.0;

    const tf1 = {};
    const tf2 = {};
    tokens1.forEach(t => tf1[t] = (tf1[t] || 0) + 1);
    tokens2.forEach(t => tf2[t] = (tf2[t] || 0) + 1);

    const vocab = new Set([...Object.keys(tf1), ...Object.keys(tf2)]);
    let dot = 0, norm1 = 0, norm2 = 0;

    vocab.forEach(term => {
      const c1 = tf1[term] || 0;
      const c2 = tf2[term] || 0;
      dot += c1 * c2;
      norm1 += c1 * c1;
      norm2 += c2 * c2;
    });

    if (norm1 === 0 || norm2 === 0) return 0.0;
    return dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
  },

  ngramAnalysis(text1, text2, n = 3) {
    const tokens1 = this.tokenize(text1);
    const tokens2 = this.tokenize(text2);
    const set1 = new Set(this.generateNGrams(tokens1, n));
    const set2 = new Set(this.generateNGrams(tokens2, n));

    if (set1.size === 0 && set2.size === 0) return { jaccard: 1.0, containment: 1.0 };
    if (set1.size === 0 || set2.size === 0) return { jaccard: 0.0, containment: 0.0 };

    let intersection = 0;
    set1.forEach(gram => {
      if (set2.has(gram)) intersection++;
    });

    const union = new Set([...set1, ...set2]).size;
    return {
      jaccard: intersection / union,
      containment: intersection / set1.size
    };
  },

  analyzeAgainstCorpus(title, inputText, corpus) {
    const totalWords = inputText ? inputText.trim().split(/\s+/).length : 0;
    let maxSim = 0, maxCosine = 0, maxNgram = 0;
    let topDoc = null;
    const allSegments = [];
    const breakdown = [];

    corpus.forEach(doc => {
      if (!doc.content) return;
      const cos = this.cosineSimilarity(inputText, doc.content) * 100;
      const ngram = this.ngramAnalysis(inputText, doc.content, 3).containment * 100;
      const combined = Math.min(100, Math.max(0, (cos * 0.45) + (ngram * 0.55)));

      const segments = this.findMatchingSegments(inputText, doc.content, doc.id, doc.title);
      breakdown.push({
        documentId: doc.id,
        title: doc.title,
        similarityScore: Math.round(combined * 10) / 10,
        cosineScore: Math.round(cos * 10) / 10,
        ngramScore: Math.round(ngram * 10) / 10,
        matchCount: segments.length
      });

      if (combined > maxSim) {
        maxSim = combined;
        maxCosine = cos;
        maxNgram = ngram;
        topDoc = doc;
      }
      allSegments.push(...segments);
    });

    breakdown.sort((a, b) => b.similarityScore - a.similarityScore);
    const overall = Math.round(maxSim * 10) / 10;
    let level = "LOW";
    if (overall > 50) level = "HIGH";
    else if (overall >= 20) level = "MEDIUM";

    const topSegments = topDoc ? this.findMatchingSegments(inputText, topDoc.content, topDoc.id, topDoc.title) : [];

    return {
      id: "REP-" + Math.random().toString(36).substring(2, 10).toUpperCase(),
      documentTitle: title || "Submitted Document",
      originalContent: inputText,
      overallSimilarity: overall,
      similarityLevel: level,
      cosineSimilarity: Math.round(maxCosine * 10) / 10,
      ngramSimilarity: Math.round(maxNgram * 10) / 10,
      totalWords,
      matchedWords: Math.min(totalWords, topSegments.reduce((acc, s) => acc + (s.sourceSnippet ? s.sourceSnippet.split(/\s+/).length : 0), 0)),
      topMatchedDocId: topDoc ? topDoc.id : null,
      topMatchedDocTitle: topDoc ? topDoc.title : null,
      matchedSegments: topSegments,
      documentBreakdown: breakdown,
      highlightedSourceHtml: this.generateHighlightedHtml(inputText, topSegments, true),
      highlightedMatchedHtml: topDoc ? this.generateHighlightedHtml(topDoc.content, topSegments, false) : "<em>No direct match.</em>",
      checkedAt: new Date().toISOString()
    };
  },

  findMatchingSegments(source, reference, refId, refTitle) {
    const segments = [];
    const srcSentences = this.splitSentences(source);
    const refSentences = this.splitSentences(reference);

    srcSentences.forEach(srcSent => {
      let bestSim = 0;
      let bestRef = null;

      refSentences.forEach(refSent => {
        const cos = this.cosineSimilarity(srcSent, refSent);
        const ngr = this.ngramAnalysis(srcSent, refSent, 2).jaccard;
        const comb = (cos * 0.5) + (ngr * 0.5);
        if (comb > bestSim) {
          bestSim = comb;
          bestRef = refSent;
        }
      });

      if (bestSim >= 0.45 && bestRef) {
        segments.push({
          sourceSnippet: srcSent,
          matchedSnippet: bestRef,
          referenceDocId: refId,
          referenceDocTitle: refTitle,
          similarityScore: Math.round(bestSim * 1000) / 10,
          matchType: bestSim >= 0.8 ? "EXACT" : (bestSim >= 0.6 ? "HIGH" : "MODERATE")
        });
      }
    });
    return segments;
  },

  generateHighlightedHtml(text, segments, isSource) {
    if (!text) return "";
    let escaped = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    segments.forEach(seg => {
      const target = isSource ? seg.sourceSnippet : seg.matchedSnippet;
      if (!target) return;
      const cleanTarget = target.trim();
      const escapedTarget = cleanTarget.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
      const tagClass = seg.matchType === 'EXACT' ? 'match-exact' : (seg.matchType === 'HIGH' ? 'match-high' : 'match-moderate');
      escaped = escaped.replace(escapedTarget, `<mark class="plag-highlight ${tagClass}" title="${seg.matchType} Match">${escapedTarget}</mark>`);
    });
    return escaped.replace(/\n/g, '<br/>');
  }
};
