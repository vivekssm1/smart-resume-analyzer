/* ════════════════════════════════════════════════════════════════════
   Smart Resume Analyzer — Frontend JavaScript
   Handles: file upload, API calls, results rendering, history
   ═══════════════════════════════════════════════════════════════════ */

// ─── CONFIG ───────────────────────────────────────────────────────────
// When running locally, API is on same origin.
// On Render, the backend serves both frontend and API.
const API_BASE = window.location.origin + '/api';

// ─── DOM ELEMENTS ─────────────────────────────────────────────────────
const fileInput       = document.getElementById('fileInput');
const dropZone        = document.getElementById('dropZone');
const filePreview     = document.getElementById('filePreview');
const fileName        = document.getElementById('fileName');
const fileSize        = document.getElementById('fileSize');
const removeFileBtn   = document.getElementById('removeFile');
const analyzeBtn      = document.getElementById('analyzeBtn');
const btnText         = document.getElementById('btnText');
const btnLoader       = document.getElementById('btnLoader');
const resultsSection  = document.getElementById('results-section');
const newAnalysisBtn  = document.getElementById('newAnalysisBtn');
const historyGrid     = document.getElementById('historyGrid');
const refreshHistory  = document.getElementById('refreshHistory');

let selectedFile = null;

// ─── FILE HANDLING ─────────────────────────────────────────────────────

fileInput.addEventListener('change', (e) => {
  handleFileSelect(e.target.files[0]);
});

// Drag & Drop
dropZone.addEventListener('dragover', (e) => {
  e.preventDefault();
  dropZone.classList.add('drag-over');
});
dropZone.addEventListener('dragleave', () => {
  dropZone.classList.remove('drag-over');
});
dropZone.addEventListener('drop', (e) => {
  e.preventDefault();
  dropZone.classList.remove('drag-over');
  const file = e.dataTransfer.files[0];
  if (file) handleFileSelect(file);
});

// Click on drop zone opens file picker
dropZone.addEventListener('click', (e) => {
  if (e.target.tagName !== 'LABEL') fileInput.click();
});

function handleFileSelect(file) {
  if (!file) return;

  const ext = file.name.split('.').pop().toLowerCase();
  if (!['pdf', 'docx'].includes(ext)) {
    showToast('Only PDF and DOCX files are supported', 'error');
    return;
  }
  if (file.size > 10 * 1024 * 1024) {
    showToast('File size must not exceed 10MB', 'error');
    return;
  }

  selectedFile = file;
  fileName.textContent = file.name;
  fileSize.textContent = formatBytes(file.size);
  filePreview.style.display = 'flex';
  dropZone.style.display = 'none';
  analyzeBtn.disabled = false;
}

removeFileBtn.addEventListener('click', () => {
  selectedFile = null;
  fileInput.value = '';
  filePreview.style.display = 'none';
  dropZone.style.display = 'flex';
  analyzeBtn.disabled = true;
});

// ─── ANALYZE ──────────────────────────────────────────────────────────

analyzeBtn.addEventListener('click', async () => {
  if (!selectedFile) return;
  await analyzeResume(selectedFile);
});

async function analyzeResume(file) {
  setLoading(true);

  const formData = new FormData();
  formData.append('file', file);

  try {
    const res = await fetch(`${API_BASE}/resumes/analyze`, {
      method: 'POST',
      body: formData
    });

    const json = await res.json();

    if (!res.ok || !json.success) {
      throw new Error(json.error || 'Analysis failed');
    }

    renderResults(json.data);
    loadHistory();
    showToast('Resume analyzed successfully! ✅', 'success');

    // Scroll to results
    setTimeout(() => {
      resultsSection.scrollIntoView({ behavior: 'smooth' });
    }, 200);

  } catch (err) {
    console.error('Analyze error:', err);
    showToast('Error: ' + err.message, 'error');
  } finally {
    setLoading(false);
  }
}

function setLoading(isLoading) {
  analyzeBtn.disabled = isLoading;
  btnText.style.display = isLoading ? 'none' : 'inline';
  btnLoader.style.display = isLoading ? 'flex' : 'none';
}

// ─── RENDER RESULTS ────────────────────────────────────────────────────

function renderResults(data) {
  resultsSection.style.display = 'block';

  // Candidate name header
  document.getElementById('resultCandidateName').textContent =
    data.candidateName || 'Unknown Candidate';

  // ATS Score gauge animation
  animateAtsScore(data.atsScore || 0);

  // Stats
  document.getElementById('expLevel').textContent = data.experienceLevel || '—';
  document.getElementById('expYears').textContent =
    data.totalExperienceYears > 0
      ? `~${data.totalExperienceYears} year(s) of experience`
      : 'No experience listed';
  document.getElementById('skillCount').textContent = data.skills?.length || 0;
  document.getElementById('wordCount').textContent = data.wordCount || 0;

  // Contact Info
  renderContactInfo(data);

  // Skills
  renderSkills(data.skills || [], data.programmingLanguages || []);

  // Education
  renderEducation(data.education || []);

  // Experience
  renderExperience(data.workExperience || []);

  // Suggestions
  renderSuggestions(data.suggestions || []);
}

function animateAtsScore(score) {
  const gaugePath = document.getElementById('gaugePath');
  const atsNumber = document.getElementById('atsNumber');
  const atsLabel  = document.getElementById('atsLabel');

  // Gauge: total arc length = 157 (half circle)
  const totalLength = 157;
  const offset = totalLength - (score / 100) * totalLength;

  let current = 0;
  const interval = setInterval(() => {
    current = Math.min(current + 2, score);
    atsNumber.textContent = current;
    const currentOffset = totalLength - (current / 100) * totalLength;
    gaugePath.style.strokeDashoffset = currentOffset;
    if (current >= score) clearInterval(interval);
  }, 20);

  if (score >= 75) atsLabel.textContent = '🟢 Excellent ATS Compatibility';
  else if (score >= 50) atsLabel.textContent = '🟡 Good — Can Be Improved';
  else atsLabel.textContent = '🔴 Needs Improvement';
}

function renderContactInfo(data) {
  const container = document.getElementById('contactInfo');
  const rows = [
    { label: 'Name',     val: data.candidateName },
    { label: 'Email',    val: data.email, link: data.email ? `mailto:${data.email}` : null },
    { label: 'Phone',    val: data.phone },
    { label: 'Location', val: data.location },
    { label: 'LinkedIn', val: data.linkedInUrl, link: data.linkedInUrl },
    { label: 'GitHub',   val: data.githubUrl,   link: data.githubUrl },
  ];

  container.innerHTML = rows.map(row => `
    <div class="info-row">
      <span class="info-label">${row.label}</span>
      <span class="info-val">
        ${row.val
          ? (row.link
              ? `<a href="${row.link}" target="_blank">${row.val}</a>`
              : escapeHtml(row.val))
          : '<span class="info-na">Not found</span>'
        }
      </span>
    </div>
  `).join('');
}

function renderSkills(skills, languages) {
  const container = document.getElementById('skillsContainer');
  if (!skills.length) {
    container.innerHTML = '<p class="entry-empty">No skills detected</p>';
    return;
  }

  const langSet = new Set(languages.map(l => l.toLowerCase()));
  const FRAMEWORKS = ['Spring Boot','React','Angular','Vue','Node.js','Django','Flask','Laravel','Next.js','Bootstrap','Hibernate','Express','FastAPI'];
  const DBS = ['MongoDB','MySQL','PostgreSQL','Redis','Oracle','Firebase','Cassandra','DynamoDB','SQLite','MariaDB','Elasticsearch'];
  const CLOUD = ['AWS','Azure','GCP','Docker','Kubernetes','Jenkins','Git','GitHub','Linux'];
  const frameworkSet = new Set(FRAMEWORKS.map(f => f.toLowerCase()));
  const dbSet = new Set(DBS.map(d => d.toLowerCase()));
  const cloudSet = new Set(CLOUD.map(c => c.toLowerCase()));

  container.innerHTML = skills.map(skill => {
    const s = skill.toLowerCase();
    let cls = 'skill-soft';
    if (langSet.has(s))     cls = 'skill-lang';
    else if (frameworkSet.has(s)) cls = 'skill-frame';
    else if (dbSet.has(s))  cls = 'skill-db';
    else if (cloudSet.has(s)) cls = 'skill-cloud';
    return `<span class="skill-tag ${cls}">${escapeHtml(skill)}</span>`;
  }).join('');
}

function renderEducation(education) {
  const container = document.getElementById('educationList');
  if (!education.length) {
    container.innerHTML = '<p class="entry-empty">No education details found</p>';
    return;
  }
  container.innerHTML = education.map(edu => `
    <div class="entry">
      <p class="entry-title">${escapeHtml(edu.degree || 'Degree')}</p>
      <p class="entry-sub">${escapeHtml(edu.institution || 'Institution not specified')}</p>
      ${edu.year ? `<p class="entry-meta">📅 ${escapeHtml(edu.year)}</p>` : ''}
    </div>
  `).join('');
}

function renderExperience(experiences) {
  const container = document.getElementById('experienceList');
  if (!experiences.length) {
    container.innerHTML = '<p class="entry-empty">No work experience found</p>';
    return;
  }
  container.innerHTML = experiences.map(exp => `
    <div class="entry">
      <p class="entry-title">${escapeHtml(exp.jobTitle || 'Role')}</p>
      <p class="entry-sub">${escapeHtml(exp.company || 'Company not specified')}</p>
      <p class="entry-meta">${escapeHtml(exp.duration || '')}</p>
    </div>
  `).join('');
}

function renderSuggestions(suggestions) {
  const list = document.getElementById('suggestionsList');
  if (!suggestions.length) {
    list.innerHTML = '<li>✅ Your resume looks great!</li>';
    return;
  }
  list.innerHTML = suggestions.map(s => `<li>${escapeHtml(s)}</li>`).join('');
}

// ─── NEW ANALYSIS ──────────────────────────────────────────────────────

newAnalysisBtn.addEventListener('click', () => {
  resultsSection.style.display = 'none';
  selectedFile = null;
  fileInput.value = '';
  filePreview.style.display = 'none';
  dropZone.style.display = 'flex';
  analyzeBtn.disabled = true;
  document.getElementById('upload').scrollIntoView({ behavior: 'smooth' });
});

// ─── HISTORY ───────────────────────────────────────────────────────────

refreshHistory.addEventListener('click', loadHistory);

async function loadHistory() {
  try {
    const res = await fetch(`${API_BASE}/resumes`);
    const json = await res.json();

    if (!json.success || !json.data.length) {
      historyGrid.innerHTML = `
        <div class="empty-state">
          <p class="empty-icon">📂</p>
          <p>No resumes analyzed yet.</p>
          <p class="empty-sub">Upload a resume above to get started!</p>
        </div>`;
      return;
    }

    historyGrid.innerHTML = json.data.reverse().map(r => {
      const scoreClass = r.atsScore >= 75 ? 'score-high' : r.atsScore >= 50 ? 'score-mid' : 'score-low';
      const topSkills = (r.skills || []).slice(0, 5);
      const date = r.uploadedAt
        ? new Date(r.uploadedAt).toLocaleDateString('en-IN', {day:'2-digit', month:'short', year:'numeric'})
        : '—';

      return `
        <div class="history-card">
          <p class="history-name">${escapeHtml(r.candidateName || 'Unknown')}</p>
          <p class="history-meta">${escapeHtml(r.fileName || '')} &nbsp;·&nbsp; ${date}</p>
          <span class="history-score ${scoreClass}">ATS: ${r.atsScore}/100</span>
          &nbsp;
          <span style="font-size:0.78rem;color:var(--text-muted);">${escapeHtml(r.experienceLevel || '')}</span>
          <div class="history-skills">
            ${topSkills.map(s => `<span class="history-skill-chip">${escapeHtml(s)}</span>`).join('')}
          </div>
          <div class="history-actions">
            <button class="btn-delete" onclick="deleteResume('${r.id}')">🗑 Delete</button>
          </div>
        </div>`;
    }).join('');

  } catch (err) {
    console.error('History error:', err);
    historyGrid.innerHTML = `<div class="empty-state"><p>Could not load history. Is the server running?</p></div>`;
  }
}

async function deleteResume(id) {
  if (!confirm('Delete this resume?')) return;
  try {
    const res = await fetch(`${API_BASE}/resumes/${id}`, { method: 'DELETE' });
    const json = await res.json();
    if (json.success) {
      showToast('Resume deleted', 'success');
      loadHistory();
    } else {
      showToast(json.error || 'Delete failed', 'error');
    }
  } catch (err) {
    showToast('Error deleting resume', 'error');
  }
}

// ─── UTILITIES ─────────────────────────────────────────────────────────

function formatBytes(bytes) {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

let toastTimer;
function showToast(message, type = '') {
  const toast = document.getElementById('toast');
  toast.textContent = message;
  toast.className = `toast ${type} show`;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    toast.classList.remove('show');
  }, 3500);
}

// ─── INIT ──────────────────────────────────────────────────────────────
loadHistory();
