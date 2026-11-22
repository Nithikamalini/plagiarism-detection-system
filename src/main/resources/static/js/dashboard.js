/**
 * Dashboard Controller
 */

document.addEventListener('DOMContentLoaded', async () => {
  if (!API.isLoggedIn()) {
    // If guest, show notice or allow exploration
    const user = API.getUser();
    if (!user) {
      document.getElementById('user-greeting').textContent = 'Guest User';
    }
  } else {
    const user = API.getUser();
    if (user && user.fullName) {
      document.getElementById('user-greeting').textContent = user.fullName;
    }
  }

  await loadDashboardStats();
  await loadSystemStatus();
  await loadRecentScans();
});

async function loadSystemStatus() {
  try {
    const sys = await API.get('/api/system/status');
    const pill = document.getElementById('server-status-pill');
    if (pill) {
      pill.innerHTML = '● ONLINE';
      pill.className = 'badge badge-low';
    }
    const setIf = (id, val) => { const el = document.getElementById(id); if (el) el.textContent = val; };
    setIf('srv-port', sys.port || 8080);
    setIf('srv-uptime', sys.uptime || 'N/A');
    setIf('srv-memory', `${sys.usedMemoryMB || 0} MB / ${sys.totalMemoryMB || 0} MB`);
    setIf('srv-db', `${sys.databaseName} (${sys.mongoConnected ? 'Active' : 'Memory Cache'})`);
    setIf('srv-threads', sys.activeThreads || 1);
    setIf('srv-java', sys.javaVersion || '17');
  } catch (err) {
    const pill = document.getElementById('server-status-pill');
    if (pill) {
      pill.innerHTML = '● OFFLINE';
      pill.className = 'badge badge-high';
    }
  }
}

async function loadDashboardStats() {
  try {
    const stats = await API.get('/api/stats');
    document.getElementById('stat-total-scans').textContent = stats.totalScans || 0;
    document.getElementById('stat-avg-similarity').textContent = (stats.avgSimilarity || 0) + '%';
    document.getElementById('stat-corpus-docs').textContent = stats.corpusDocuments || 0;
    document.getElementById('stat-flagged-docs').textContent = (stats.highRiskCount || 0) + (stats.mediumRiskCount || 0);

    const mongoStatusBadge = document.getElementById('mongo-status');
    if (mongoStatusBadge) {
      if (stats.isMongoConnected) {
        mongoStatusBadge.innerHTML = '<span style="color:#10b981;">●</span> MongoDB Connected';
        mongoStatusBadge.className = 'badge badge-low';
      } else {
        mongoStatusBadge.innerHTML = '<span style="color:#f59e0b;">●</span> Memory Active (Mongo Standby)';
        mongoStatusBadge.className = 'badge badge-medium';
      }
    }
  } catch (err) {
    console.warn('Could not load stats:', err);
  }
}

async function loadRecentScans() {
  const tbody = document.getElementById('recent-scans-body');
  if (!tbody) return;

  try {
    const reports = await API.get('/api/history');
    if (!reports || reports.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; color:var(--text-muted); padding:30px;">No scans recorded yet. <a href="scan.html" style="color:var(--secondary);">Run your first plagiarism check</a></td></tr>`;
      return;
    }

    const recent = reports.slice(0, 5);
    tbody.innerHTML = recent.map(r => {
      let badgeClass = 'badge-low';
      if (r.similarityLevel === 'MEDIUM') badgeClass = 'badge-medium';
      if (r.similarityLevel === 'HIGH') badgeClass = 'badge-high';

      const dateStr = r.checkedAt ? new Date(r.checkedAt).toLocaleString() : 'Recent';

      return `
        <tr>
          <td>
            <strong>${escapeHtml(r.documentTitle || 'Untitled')}</strong>
            <div style="font-size:0.75rem; color:var(--text-muted);">${dateStr}</div>
          </td>
          <td>
            <span class="badge ${badgeClass}">${r.similarityLevel} (${r.overallSimilarity}%)</span>
          </td>
          <td>
            <div style="font-size:0.85rem;">Cosine: <strong>${r.cosineSimilarity}%</strong></div>
            <div style="font-size:0.85rem;">N-Gram: <strong>${r.ngramSimilarity}%</strong></div>
          </td>
          <td>
            ${r.topMatchedDocTitle ? escapeHtml(r.topMatchedDocTitle) : '<span style="color:var(--text-muted);">None</span>'}
          </td>
          <td>
            <a href="result.html?id=${r.id}" class="btn btn-outline btn-sm">View Report</a>
          </td>
        </tr>
      `;
    }).join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; color:var(--danger);">Failed to load recent scans.</td></tr>`;
  }
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/[&<>"']/g, m => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[m]);
}
