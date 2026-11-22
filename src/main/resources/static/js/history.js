/**
 * History Controller
 */

let allHistory = [];

document.addEventListener('DOMContentLoaded', async () => {
  await loadHistory();
  setupFilters();
});

async function loadHistory() {
  const tbody = document.getElementById('history-tbody');
  if (!tbody) return;

  try {
    const list = await API.get('/api/history');
    allHistory = list || [];
    renderHistoryTable(allHistory);
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:var(--danger);">Error loading history: ${err.message}</td></tr>`;
  }
}

function renderHistoryTable(items) {
  const tbody = document.getElementById('history-tbody');
  if (!tbody) return;

  if (items.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:var(--text-muted); padding:40px;">No scan history found.</td></tr>`;
    return;
  }

  tbody.innerHTML = items.map(r => {
    let badgeClass = 'badge-low';
    if (r.similarityLevel === 'MEDIUM') badgeClass = 'badge-medium';
    if (r.similarityLevel === 'HIGH') badgeClass = 'badge-high';

    const dateStr = r.checkedAt ? new Date(r.checkedAt).toLocaleString() : 'N/A';

    return `
      <tr>
        <td>
          <strong>${escapeHtml(r.documentTitle || 'Untitled')}</strong>
          <div style="font-size:0.75rem; color:var(--text-muted); font-family:monospace;">${r.id}</div>
        </td>
        <td>${dateStr}</td>
        <td>
          <span class="badge ${badgeClass}">${r.similarityLevel} (${r.overallSimilarity}%)</span>
        </td>
        <td>
          <div style="font-size:0.8rem;">Cosine: ${r.cosineSimilarity}%</div>
          <div style="font-size:0.8rem;">N-Gram: ${r.ngramSimilarity}%</div>
        </td>
        <td>${r.topMatchedDocTitle ? escapeHtml(r.topMatchedDocTitle) : '<span style="color:var(--text-muted);">None</span>'}</td>
        <td>
          <div style="display:flex; gap:6px;">
            <a href="result.html?id=${r.id}" class="btn btn-outline btn-sm" title="View Detailed Report">View</a>
            <a href="/api/report/printable?id=${r.id}" target="_blank" class="btn btn-secondary btn-sm" title="Print/PDF Report">PDF</a>
            <a href="/api/report/export?id=${r.id}&format=csv" class="btn btn-outline btn-sm" title="Download CSV">CSV</a>
            <button onclick="deleteHistoryItem('${r.id}')" class="btn btn-outline btn-sm" style="color:var(--danger); border-color:var(--danger);" title="Delete">🗑️</button>
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

function setupFilters() {
  const searchInput = document.getElementById('history-search');
  const levelFilter = document.getElementById('history-level-filter');

  const filterAction = () => {
    const q = (searchInput?.value || '').toLowerCase().trim();
    const lvl = levelFilter?.value || 'ALL';

    const filtered = allHistory.filter(item => {
      const matchText = (item.documentTitle || '').toLowerCase().includes(q) || (item.id || '').toLowerCase().includes(q);
      const matchLvl = (lvl === 'ALL') || (item.similarityLevel === lvl);
      return matchText && matchLvl;
    });

    renderHistoryTable(filtered);
  };

  if (searchInput) searchInput.addEventListener('input', filterAction);
  if (levelFilter) levelFilter.addEventListener('change', filterAction);
}

async function deleteHistoryItem(id) {
  if (!confirm(`Are you sure you want to delete scan record ${id}?`)) return;

  try {
    await API.delete(`/api/history?id=${id}`);
    API.showToast('Record deleted.', 'info');
    allHistory = allHistory.filter(h => h.id !== id);
    renderHistoryTable(allHistory);
  } catch (err) {
    API.showToast('Failed to delete: ' + err.message, 'error');
  }
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/[&<>"']/g, m => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[m]);
}
