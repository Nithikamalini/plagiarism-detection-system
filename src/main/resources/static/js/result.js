/**
 * Result Controller - Interactive Plagiarism Report
 */

let currentReport = null;

document.addEventListener('DOMContentLoaded', async () => {
  const urlParams = new URLSearchParams(window.location.search);
  const reportId = urlParams.get('id');

  if (!reportId) {
    API.showToast('No report ID specified. Redirecting to scan...', 'error');
    setTimeout(() => { window.location.href = 'scan.html'; }, 1000);
    return;
  }

  await loadReport(reportId);
});

async function loadReport(reportId) {
  try {
    const report = await API.get(`/api/history?id=${reportId}`);
    if (!report || !report.id) {
      document.getElementById('report-container').innerHTML = `
        <div class="card" style="text-align:center; padding:50px;">
          <h2 style="color:var(--danger);">Report Not Found</h2>
          <p style="color:var(--text-muted); margin:15px 0;">The requested plagiarism report does not exist or was deleted.</p>
          <a href="scan.html" class="btn btn-primary">Start New Scan</a>
        </div>
      `;
      return;
    }

    currentReport = report;
    renderReport(report);
  } catch (err) {
    API.showToast('Error loading report: ' + err.message, 'error');
  }
}

function renderReport(r) {
  // Title & ID
  document.getElementById('report-doc-title').textContent = r.documentTitle || 'Untitled Document';
  document.getElementById('report-id').textContent = r.id;
  document.getElementById('report-timestamp').textContent = r.checkedAt ? new Date(r.checkedAt).toLocaleString() : 'Just now';

  // Metrics
  document.getElementById('metric-cosine').textContent = (r.cosineSimilarity || 0) + '%';
  document.getElementById('metric-ngram').textContent = (r.ngramSimilarity || 0) + '%';
  document.getElementById('metric-total-words').textContent = r.totalWords || 0;
  document.getElementById('metric-matched-words').textContent = r.matchedWords || 0;

  // Level & Gauge
  const sim = r.overallSimilarity || 0;
  renderGauge(sim, r.similarityLevel);

  // Highlighting
  const sourceViewer = document.getElementById('highlighted-source');
  sourceViewer.innerHTML = r.highlightedSourceHtml || escapeHtml(r.originalContent || '');

  const matchedViewer = document.getElementById('highlighted-matched');
  matchedViewer.innerHTML = r.highlightedMatchedHtml || '<em>No direct matches found.</em>';

  const refTitleHeader = document.getElementById('matched-doc-header-title');
  if (refTitleHeader) {
    refTitleHeader.textContent = r.topMatchedDocTitle ? `Reference: ${r.topMatchedDocTitle}` : 'No Matched Document';
  }

  // Matched Segments List
  renderMatchedSegments(r.matchedSegments || []);

  // Document Breakdown List
  renderDocumentBreakdown(r.documentBreakdown || []);
}

function renderGauge(score, level) {
  const circle = document.getElementById('gauge-progress');
  const scoreNum = document.getElementById('gauge-score-num');
  const levelBadge = document.getElementById('gauge-level-badge');

  let strokeColor = '#10b981'; // Green
  let badgeClass = 'badge-low';

  if (level === 'MEDIUM') {
    strokeColor = '#f59e0b'; // Amber
    badgeClass = 'badge-medium';
  } else if (level === 'HIGH') {
    strokeColor = '#ef4444'; // Red
    badgeClass = 'badge-high';
  }

  // SVG circle calculations: r = 70, circumference = 2 * PI * 70 = 439.82
  const circumference = 2 * Math.PI * 70;
  circle.style.strokeDasharray = `${circumference} ${circumference}`;

  const offset = circumference - (score / 100) * circumference;
  circle.style.stroke = strokeColor;
  circle.style.strokeDashoffset = offset;

  scoreNum.textContent = score + '%';
  scoreNum.style.color = strokeColor;

  levelBadge.textContent = `${level} SIMILARITY`;
  levelBadge.className = `badge ${badgeClass}`;
}

function renderMatchedSegments(segments) {
  const container = document.getElementById('matched-segments-list');
  if (!container) return;

  if (segments.length === 0) {
    container.innerHTML = `<p style="color:var(--text-muted); font-size:0.9rem;">No substantial plagiarized phrases or sentences detected.</p>`;
    return;
  }

  container.innerHTML = segments.map((seg, idx) => {
    let tagClass = 'match-moderate';
    if (seg.matchType === 'EXACT') tagClass = 'match-exact';
    else if (seg.matchType === 'HIGH') tagClass = 'match-high';

    return `
      <div style="background:rgba(15,23,42,0.6); border:1px solid var(--border); border-radius:var(--radius-sm); padding:12px; margin-bottom:10px;">
        <div style="display:flex; justify-content:space-between; margin-bottom:6px; font-size:0.8rem;">
          <span class="plag-highlight ${tagClass}">Match #${idx + 1} (${seg.matchType})</span>
          <span style="color:var(--text-muted);">Confidence: <strong>${seg.similarityScore}%</strong></span>
        </div>
        <div style="font-size:0.9rem; color:#f8fafc; margin-bottom:4px;"><strong>Submitted:</strong> "${escapeHtml(seg.sourceSnippet)}"</div>
        <div style="font-size:0.85rem; color:var(--text-muted);"><strong>Matched In:</strong> <em>${escapeHtml(seg.referenceDocTitle)}</em></div>
      </div>
    `;
  }).join('');
}

function renderDocumentBreakdown(breakdown) {
  const tbody = document.getElementById('breakdown-tbody');
  if (!tbody) return;

  if (breakdown.length === 0) {
    tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; color:var(--text-muted);">No comparison documents in corpus.</td></tr>`;
    return;
  }

  tbody.innerHTML = breakdown.map(doc => {
    let barColor = '#10b981';
    if (doc.similarityScore > 50) barColor = '#ef4444';
    else if (doc.similarityScore > 20) barColor = '#f59e0b';

    return `
      <tr>
        <td><strong>${escapeHtml(doc.title)}</strong></td>
        <td>
          <div style="display:flex; align-items:center; gap:10px;">
            <div style="flex:1; background:var(--border); height:8px; border-radius:4px; overflow:hidden;">
              <div style="background:${barColor}; width:${Math.min(100, doc.similarityScore)}%; height:100%;"></div>
            </div>
            <span style="font-weight:700; width:45px;">${doc.similarityScore}%</span>
          </div>
        </td>
        <td>${doc.cosineScore}%</td>
        <td>${doc.ngramScore}%</td>
        <td><span class="badge badge-low">${doc.matchCount} snippets</span></td>
      </tr>
    `;
  }).join('');
}

let panelsExpanded = false;

function toggleExpandPanels() {
  const bodies = document.querySelectorAll('.comparison-body');
  const btn = document.getElementById('toggle-expand-btn');
  panelsExpanded = !panelsExpanded;

  bodies.forEach(b => {
    if (panelsExpanded) {
      b.style.maxHeight = 'none';
    } else {
      b.style.maxHeight = '520px';
    }
  });

  if (btn) {
    btn.innerHTML = panelsExpanded ? '↕️ Collapse View' : '↕️ Expand Full Height (Show All Data)';
  }
}

function openPrintableReport() {
  if (!currentReport) return;
  window.open(`/api/report/printable?id=${currentReport.id}`, '_blank');
}

function downloadCsvReport() {
  if (!currentReport) return;
  window.location.href = `/api/report/export?id=${currentReport.id}&format=csv`;
}

function downloadTxtReport() {
  if (!currentReport) return;
  window.location.href = `/api/report/export?id=${currentReport.id}&format=txt`;
}

function downloadJsonReport() {
  if (!currentReport) return;
  const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(currentReport, null, 2));
  const downloadAnchor = document.createElement('a');
  downloadAnchor.setAttribute("href", dataStr);
  downloadAnchor.setAttribute("download", `PlagiarismReport-${currentReport.id}.json`);
  document.body.appendChild(downloadAnchor);
  downloadAnchor.click();
  downloadAnchor.remove();
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/[&<>"']/g, m => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[m]);
}
