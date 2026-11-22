/**
 * Corpus Management Controller
 */

let allCorpusDocs = [];

document.addEventListener('DOMContentLoaded', async () => {
  await loadCorpus();
});

async function loadCorpus() {
  const container = document.getElementById('corpus-docs-list');
  if (!container) return;

  try {
    allCorpusDocs = await API.get('/api/corpus');
    document.getElementById('corpus-count').textContent = (allCorpusDocs ? allCorpusDocs.length : 0) + ' documents';
    renderCorpusList(allCorpusDocs);
  } catch (err) {
    container.innerHTML = `<p style="color:var(--danger); padding:20px;">Failed to load documents: ${err.message}</p>`;
  }
}

const docsMap = {};

function renderCorpusList(docs) {
  const container = document.getElementById('corpus-docs-list');
  if (!container) return;

  if (!docs || docs.length === 0) {
    container.innerHTML = `<p style="color:var(--text-muted); text-align:center; padding:40px;">No documents in corpus. Add your first document above.</p>`;
    return;
  }

  docs.forEach(d => { docsMap[d.id] = d; });

  container.innerHTML = docs.map(d => {
    const fullContent = d.content || '';
    const isLong = fullContent.length > 250;
    const shortContent = isLong ? fullContent.substring(0, 250) + '...' : fullContent;

    return `
      <div class="card" style="margin-bottom:16px;">
        <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:10px; flex-wrap:wrap; gap:10px;">
          <div>
            <h3 style="font-size:1.15rem; color:#fff;">${escapeHtml(d.title)}</h3>
            <div style="font-size:0.8rem; color:var(--text-muted); margin-top:4px;">
              <span class="badge badge-low">${escapeHtml(d.category || 'General')}</span>
              <span style="margin-left:8px;">${d.wordCount} words</span>
              <span style="margin-left:8px; font-family:monospace;">${d.id}</span>
            </div>
          </div>
          <div style="display:flex; gap:8px;">
            <button onclick="viewFullDoc('${d.id}')" class="btn btn-outline btn-sm">📖 View Full Text</button>
            <button onclick="deleteCorpusDoc('${d.id}')" class="btn btn-outline btn-sm" style="color:var(--danger); border-color:var(--danger);">Delete</button>
          </div>
        </div>
        
        <div style="background:rgba(15,23,42,0.5); padding:16px; border-radius:var(--radius-sm); border:1px solid rgba(51,65,85,0.4); line-height:1.7; font-size:0.95rem; color:#cbd5e1; white-space:pre-wrap;">
          <div id="content-short-${d.id}">${escapeHtml(shortContent)}</div>
          <div id="content-full-${d.id}" style="display:none;">${escapeHtml(fullContent)}</div>
          ${isLong ? `
            <button id="toggle-btn-${d.id}" onclick="toggleDocInline('${d.id}')" class="btn btn-outline btn-sm" style="margin-top:10px; padding:4px 10px; font-size:0.8rem;">
              ⬇️ Show All Text (${d.wordCount} words)
            </button>
          ` : ''}
        </div>
      </div>
    `;
  }).join('');
}

function toggleDocInline(id) {
  const shortDiv = document.getElementById(`content-short-${id}`);
  const fullDiv = document.getElementById(`content-full-${id}`);
  const btn = document.getElementById(`toggle-btn-${id}`);

  if (fullDiv.style.display === 'none') {
    fullDiv.style.display = 'block';
    shortDiv.style.display = 'none';
    btn.innerHTML = '⬆️ Show Less';
  } else {
    fullDiv.style.display = 'none';
    shortDiv.style.display = 'block';
    btn.innerHTML = '⬇️ Show All Text';
  }
}

function viewFullDoc(id) {
  const doc = docsMap[id];
  if (!doc) return;

  document.getElementById('view-doc-title').textContent = doc.title || 'Untitled';
  document.getElementById('view-doc-cat').textContent = doc.category || 'General';
  document.getElementById('view-doc-words').textContent = `${doc.wordCount} words`;
  document.getElementById('view-doc-id').textContent = doc.id;
  document.getElementById('view-doc-body').textContent = doc.content || '(Empty document)';

  document.getElementById('view-modal').classList.add('active');
}

function closeViewModal() {
  document.getElementById('view-modal').classList.remove('active');
}

function copyDocumentText() {
  const text = document.getElementById('view-doc-body').textContent;
  navigator.clipboard.writeText(text).then(() => {
    API.showToast('Copied full document to clipboard!', 'success');
  });
}

async function handleCorpusFileUpload(event) {
  const file = event.target.files[0];
  if (!file) return;

  const titleInput = document.getElementById('new-doc-title');
  const contentArea = document.getElementById('new-doc-content');

  if (titleInput && (!titleInput.value || titleInput.value.trim() === '')) {
    titleInput.value = file.name.replace(/\.[^/.]+$/, "");
  }

  const fileName = file.name.toLowerCase();
  API.showToast(`Extracting data from ${file.name}...`, 'info');

  try {
    let text = '';
    if (fileName.endsWith('.docx') || fileName.endsWith('.doc')) {
      if (window.mammoth) {
        const buffer = await file.arrayBuffer();
        const res = await mammoth.extractRawText({ arrayBuffer: buffer });
        text = res.value;
      }
    } else if (fileName.endsWith('.pdf')) {
      if (window.pdfjsLib) {
        pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js';
        const buffer = await file.arrayBuffer();
        const pdf = await pdfjsLib.getDocument({ data: new Uint8Array(buffer) }).promise;
        for (let i = 1; i <= pdf.numPages; i++) {
          const page = await pdf.getPage(i);
          const content = await page.getTextContent();
          text += content.items.map(s => s.str).join(' ') + '\n\n';
        }
      }
    } else if (fileName.endsWith('.png') || fileName.endsWith('.jpg') || fileName.endsWith('.jpeg')) {
      if (window.Tesseract) {
        API.showToast('Running OCR on image...', 'info');
        const res = await Tesseract.recognize(file, 'eng');
        text = res.data.text;
      }
    } else {
      text = await file.text();
    }

    if (contentArea && text) {
      contentArea.value = text.trim();
      API.showToast(`Extracted 100% of data from ${file.name}!`, 'success');
    }
  } catch (err) {
    console.error(err);
    API.showToast('Could not extract text: ' + err.message, 'error');
  }
}

async function addCorpusDoc(e) {
  e.preventDefault();
  const title = document.getElementById('new-doc-title').value.trim();
  const category = document.getElementById('new-doc-category').value.trim();
  const content = document.getElementById('new-doc-content').value.trim();

  if (!title || !content) {
    API.showToast('Title and content are required.', 'error');
    return;
  }

  try {
    const result = await API.post('/api/corpus', { title, category, content });
    if (result.success) {
      API.showToast('Document added to MongoDB corpus!', 'success');
      document.getElementById('add-doc-form').reset();
      closeModal();
      await loadCorpus();
    } else {
      API.showToast(result.message || 'Failed to add document.', 'error');
    }
  } catch (err) {
    API.showToast('Error: ' + err.message, 'error');
  }
}

async function deleteCorpusDoc(id) {
  if (!confirm('Are you sure you want to remove this document from the comparison corpus?')) return;

  try {
    await API.delete(`/api/corpus?id=${id}`);
    API.showToast('Document removed.', 'info');
    allCorpusDocs = allCorpusDocs.filter(d => d.id !== id);
    renderCorpusList(allCorpusDocs);
    document.getElementById('corpus-count').textContent = allCorpusDocs.length + ' documents';
  } catch (err) {
    API.showToast('Delete failed: ' + err.message, 'error');
  }
}

function openModal() {
  document.getElementById('add-modal').classList.add('active');
}

function closeModal() {
  document.getElementById('add-modal').classList.remove('active');
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/[&<>"']/g, m => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[m]);
}
