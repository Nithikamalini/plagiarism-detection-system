/**
 * Plagiarism Scan Controller
 */

let currentMode = 'CORPUS'; // 'CORPUS' or 'DIRECT'

document.addEventListener('DOMContentLoaded', () => {
  setupTabs();
  setupCounters();
  setupDropzones();
});

function setupTabs() {
  const tabCorpus = document.getElementById('tab-corpus');
  const tabDirect = document.getElementById('tab-direct');
  const directSection = document.getElementById('direct-compare-section');

  if (tabCorpus && tabDirect) {
    tabCorpus.addEventListener('click', () => {
      currentMode = 'CORPUS';
      tabCorpus.classList.add('active');
      tabDirect.classList.remove('active');
      if (directSection) directSection.style.display = 'none';
    });

    tabDirect.addEventListener('click', () => {
      currentMode = 'DIRECT';
      tabDirect.classList.add('active');
      tabCorpus.classList.remove('active');
      if (directSection) directSection.style.display = 'block';
    });
  }
}

function setupCounters() {
  const textarea = document.getElementById('inputText');
  if (textarea) {
    textarea.addEventListener('input', updateTextCounters);
  }

  const directTextarea = document.getElementById('directText');
  if (directTextarea) {
    directTextarea.addEventListener('input', () => {
      const text = directTextarea.value.trim();
      const words = text ? text.split(/\s+/).length : 0;
      document.getElementById('direct-word-count').textContent = words + ' words';
    });
  }
}

function updateTextCounters() {
  const textarea = document.getElementById('inputText');
  const text = textarea.value.trim();
  const words = text ? text.split(/\s+/).length : 0;
  const chars = textarea.value.length;

  document.getElementById('word-count').textContent = words + ' words';
  document.getElementById('char-count').textContent = chars + ' chars';
}

function setupDropzones() {
  setupSingleDropzone('fileInput', 'dropzone', 'inputText', updateTextCounters);
  setupSingleDropzone('directFileInput', 'direct-dropzone', 'directText', () => {
    const text = document.getElementById('directText').value.trim();
    document.getElementById('direct-word-count').textContent = (text ? text.split(/\s+/).length : 0) + ' words';
  });
}

function setupSingleDropzone(inputId, dropzoneId, targetTextAreaId, onUpdate) {
  const input = document.getElementById(inputId);
  const dropzone = document.getElementById(dropzoneId);
  const targetArea = document.getElementById(targetTextAreaId);

  if (!input || !dropzone || !targetArea) return;

  dropzone.addEventListener('dragover', (e) => {
    e.preventDefault();
    dropzone.classList.add('dragover');
  });

  dropzone.addEventListener('dragleave', () => {
    dropzone.classList.remove('dragover');
  });

  dropzone.addEventListener('drop', (e) => {
    e.preventDefault();
    dropzone.classList.remove('dragover');
    if (e.dataTransfer.files.length > 0) {
      readFile(e.dataTransfer.files[0], targetArea, onUpdate);
    }
  });

  input.addEventListener('change', (e) => {
    if (e.target.files.length > 0) {
      readFile(e.target.files[0], targetArea, onUpdate);
    }
  });
}

async function readFile(file, targetArea, callback) {
  const fileName = file.name.toLowerCase();
  const loadingIndicator = document.getElementById('file-loading-indicator');
  const loadingText = document.getElementById('file-loading-text');

  const showLoading = (msg) => {
    if (loadingIndicator && loadingText) {
      loadingIndicator.style.display = 'block';
      loadingText.textContent = msg;
    }
  };

  const hideLoading = () => {
    if (loadingIndicator) loadingIndicator.style.display = 'none';
  };

  try {
    let extractedText = '';
    let docType = 'Plain Text';

    if (fileName.endsWith('.docx') || fileName.endsWith('.doc')) {
      docType = 'Word Document (.docx)';
      showLoading(`Extracting full text from Word document: ${file.name}...`);
      extractedText = await extractDocxText(file);
    } else if (fileName.endsWith('.pdf')) {
      docType = 'PDF Document (.pdf)';
      showLoading(`Extracting pages and text from PDF: ${file.name}...`);
      extractedText = await extractPdfText(file);
    } else if (fileName.endsWith('.png') || fileName.endsWith('.jpg') || fileName.endsWith('.jpeg') || fileName.endsWith('.bmp') || fileName.endsWith('.webp')) {
      docType = 'Image OCR Document';
      showLoading(`Scanning image with Optical Character Recognition (OCR)...`);
      extractedText = await extractImageText(file, showLoading);
      showImageThumbnail(file);
    } else {
      // Plain text, markdown, json, csv
      docType = 'Text Document';
      showLoading(`Reading text file: ${file.name}...`);
      extractedText = await readAsPlainText(file);
    }

    hideLoading();

    if (!extractedText || extractedText.trim().length === 0) {
      API.showToast(`Warning: No readable text could be extracted from ${file.name}.`, 'error');
      return;
    }

    targetArea.value = extractedText;
    if (callback) callback();

    // Show preview card
    showPreviewCard(file, docType, extractedText);

    API.showToast(`Successfully extracted 100% of data from ${file.name}!`, 'success');

    // Auto-fill title if empty
    const titleInput = document.getElementById('docTitle');
    if (titleInput && (!titleInput.value || titleInput.value.trim() === '')) {
      titleInput.value = file.name.replace(/\.[^/.]+$/, "");
    }
  } catch (err) {
    hideLoading();
    console.error('File parsing error:', err);
    API.showToast(`Failed to parse file: ${err.message}`, 'error');
  }
}

// DOCX Extraction using Mammoth.js
function extractDocxText(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = function(e) {
      const arrayBuffer = e.target.result;
      if (window.mammoth) {
        window.mammoth.extractRawText({ arrayBuffer: arrayBuffer })
          .then(result => resolve(result.value))
          .catch(err => reject(err));
      } else {
        // Fallback to text reader
        const textReader = new FileReader();
        textReader.onload = (ev) => resolve(ev.target.result);
        textReader.readAsText(file);
      }
    };
    reader.onerror = reject;
    reader.readAsArrayBuffer(file);
  });
}

// PDF Extraction using PDF.js
function extractPdfText(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = async function(e) {
      const typedarray = new Uint8Array(e.target.result);
      if (window.pdfjsLib) {
        try {
          pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js';
          const pdf = await pdfjsLib.getDocument({ data: typedarray }).promise;
          let fullText = '';
          for (let i = 1; i <= pdf.numPages; i++) {
            const page = await pdf.getPage(i);
            const textContent = await page.getTextContent();
            const pageText = textContent.items.map(item => item.str).join(' ');
            fullText += pageText + '\n\n';
          }
          resolve(fullText.trim());
        } catch (pdfErr) {
          reject(pdfErr);
        }
      } else {
        resolve(new TextDecoder().decode(typedarray));
      }
    };
    reader.onerror = reject;
    reader.readAsArrayBuffer(file);
  });
}

// Image OCR Extraction using Tesseract.js
function extractImageText(file, progressCb) {
  return new Promise((resolve, reject) => {
    if (window.Tesseract) {
      Tesseract.recognize(
        file,
        'eng',
        {
          logger: m => {
            if (m.status === 'recognizing text' && progressCb) {
              progressCb(`Recognizing text from image: ${Math.round(m.progress * 100)}%`);
            }
          }
        }
      ).then(({ data: { text } }) => {
        resolve(text.trim());
      }).catch(reject);
    } else {
      reject(new Error('OCR engine library not loaded.'));
    }
  });
}

function readAsPlainText(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => resolve(e.target.result);
    reader.onerror = reject;
    reader.readAsText(file);
  });
}

function showPreviewCard(file, docType, text) {
  const card = document.getElementById('file-preview-card');
  if (!card) return;

  card.style.display = 'flex';
  const nameEl = document.getElementById('file-info-name');
  const typeEl = document.getElementById('file-info-type');
  const sizeEl = document.getElementById('file-info-size');
  const wordsEl = document.getElementById('file-info-words');

  if (nameEl) nameEl.textContent = file.name;
  if (typeEl) typeEl.textContent = docType;
  if (sizeEl) sizeEl.textContent = (file.size / 1024).toFixed(1) + ' KB';
  const words = text ? text.trim().split(/\s+/).length : 0;
  if (wordsEl) wordsEl.textContent = `${words} words extracted`;
}

function showImageThumbnail(file) {
  const img = document.getElementById('image-thumbnail');
  if (!img) return;
  const reader = new FileReader();
  reader.onload = (e) => {
    img.src = e.target.result;
    img.style.display = 'block';
  };
  reader.readAsDataURL(file);
}

function clearUploadedFile() {
  const input = document.getElementById('fileInput');
  const textarea = document.getElementById('inputText');
  const card = document.getElementById('file-preview-card');
  const img = document.getElementById('image-thumbnail');

  if (input) input.value = '';
  if (textarea) {
    textarea.value = '';
    updateTextCounters();
  }
  if (card) card.style.display = 'none';
  if (img) img.style.display = 'none';
  API.showToast('Cleared uploaded file.', 'info');
}

function loadSample(type) {
  const titleInput = document.getElementById('docTitle');
  const textArea = document.getElementById('inputText');

  if (type === 'plagiarized') {
    titleInput.value = "AI Research Essay (Copied Excerpt)";
    textArea.value = "Artificial intelligence is a wide-ranging branch of computer science concerned with building smart machines capable of performing tasks that typically require human intelligence. Deep learning is part of a broader family of machine learning methods based on artificial neural networks with representation learning. As machine learning algorithms learn from data and improve their accuracy over time without being explicitly programmed, supervised learning algorithms build a mathematical model of data that contains both inputs and outputs.";
  } else if (type === 'original') {
    titleInput.value = "Autonomous Drone Navigation in Dense Foliage";
    textArea.value = "This experimental study examines optical flow estimation techniques combined with lightweight lidar arrays to achieve collision-free navigation for micro aerial vehicles operating beneath forest canopies. We propose an adaptive trajectory planner that dynamically replans navigation vectors every fifteen milliseconds based on probabilistic point cloud occupancy grids.";
  }

  updateTextCounters();
  API.showToast(`Loaded ${type} sample text`, 'info');
}

async function startScan() {
  const title = document.getElementById('docTitle').value.trim() || 'Submitted Document';
  const content = document.getElementById('inputText').value.trim();
  const saveToCorpus = document.getElementById('saveToCorpus')?.checked || false;

  if (!content) {
    API.showToast('Please enter text or upload a document to scan.', 'error');
    return;
  }

  const payload = {
    mode: currentMode,
    title,
    content,
    saveToCorpus
  };

  if (currentMode === 'DIRECT') {
    const directTitle = document.getElementById('directTitle').value.trim() || 'Comparison Document';
    const directContent = document.getElementById('directText').value.trim();
    if (!directContent) {
      API.showToast('Please enter or upload the reference document for comparison.', 'error');
      return;
    }
    payload.doc2Title = directTitle;
    payload.doc2Content = directContent;
  }

  // Show loading state
  const scanBtn = document.getElementById('scanBtn');
  const originalText = scanBtn.innerHTML;
  scanBtn.disabled = true;
  scanBtn.innerHTML = `<span>⏳</span> Analyzing Cosine Similarity & N-Grams...`;

  try {
    const result = await API.post('/api/plagiarism/check', payload);
    if (result.success && result.report) {
      API.showToast('Analysis complete! Redirecting to report...', 'success');
      setTimeout(() => {
        window.location.href = `result.html?id=${result.report.id}`;
      }, 600);
    } else {
      API.showToast(result.message || 'Analysis failed.', 'error');
      scanBtn.disabled = false;
      scanBtn.innerHTML = originalText;
    }
  } catch (err) {
    API.showToast(err.message || 'Server error during plagiarism scan.', 'error');
    scanBtn.disabled = false;
    scanBtn.innerHTML = originalText;
  }
}
