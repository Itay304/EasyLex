(function () {
  const listIdInput = document.getElementById('listIdInput');
  const listOptions = document.getElementById('listOptions');
  const fileInput = document.getElementById('fileInput');
  const refreshListsBtn = document.getElementById('refreshListsBtn');
  const refreshDupBtn = document.getElementById('refreshDupBtn');
  const selectAllBtn = document.getElementById('selectAllBtn');
  const deselectAllBtn = document.getElementById('deselectAllBtn');
  const uploadBtn = document.getElementById('uploadBtn');
  const statusBar = document.getElementById('statusBar');
  const progressWrap = document.getElementById('progressWrap');
  const progressFill = document.getElementById('progressFill');
  const progressText = document.getElementById('progressText');
  const tableBody = document.getElementById('wordsTableBody');
  const emptyMsg = document.getElementById('emptyMsg');

  const FIELDS = [
    'englishWord',
    'partOfSpeech',
    'hebrewTranslation',
    'exampleSentence',
    'hebrewExample',
  ];

  /** @type {Array<{id:number, englishWord:string, partOfSpeech:string, hebrewTranslation:string, exampleSentence:string, hebrewExample:string, difficulty:number, tags:string, include:boolean, isDuplicate:boolean}>} */
  let rows = [];
  let nextId = 1;
  let existingWordsSet = new Set();

  function showStatus(message, type) {
    statusBar.textContent = message;
    statusBar.className = 'status ' + type;
    statusBar.classList.remove('hidden');
  }

  function hideStatus() {
    statusBar.classList.add('hidden');
  }

  function setControlsEnabled(enabled) {
    refreshDupBtn.disabled = !enabled;
    selectAllBtn.disabled = !enabled;
    deselectAllBtn.disabled = !enabled;
    uploadBtn.disabled = !enabled;
  }

  function normalizeWord(raw) {
    return {
      id: nextId++,
      englishWord: String(raw.englishWord || '').trim(),
      partOfSpeech: String(raw.partOfSpeech || '').trim(),
      hebrewTranslation: String(raw.hebrewTranslation || '').trim(),
      exampleSentence: String(raw.exampleSentence || '').trim(),
      hebrewExample: String(raw.hebrewExample || '').trim(),
      difficulty: Number.isFinite(Number(raw.difficulty)) && Number(raw.difficulty) > 0 ? Number(raw.difficulty) : 1,
      tags: String(raw.tags || '').trim(),
      include: true,
      isDuplicate: false,
    };
  }

  async function loadLists() {
    try {
      const res = await fetch('/lists');
      if (!res.ok) throw new Error((await res.json()).error || res.statusText);
      const data = await res.json();
      listOptions.innerHTML = '';
      (data.lists || []).forEach((list) => {
        const option = document.createElement('option');
        option.value = list.id;
        option.textContent = `${list.id} (${list.wordCount} מילים)`;
        listOptions.appendChild(option);
      });
    } catch (err) {
      console.error(err);
      showStatus('שגיאה בטעינת רשימות: ' + err.message, 'error');
    }
  }

  refreshListsBtn.addEventListener('click', loadLists);
  loadLists();

  fileInput.addEventListener('change', async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    try {
      const text = await file.text();
      const parsed = JSON.parse(text);
      if (!Array.isArray(parsed)) {
        throw new Error('הקובץ חייב להכיל מערך (array) של מילים');
      }
      rows = parsed.map(normalizeWord);
      setControlsEnabled(rows.length > 0);
      render();
      showStatus(`נטענו ${rows.length} מילים מהקובץ.`, 'info');
      await refreshDuplicates();
    } catch (err) {
      console.error(err);
      showStatus('שגיאה בטעינת הקובץ: ' + err.message, 'error');
    }
  });

  async function refreshDuplicates() {
    const listId = listIdInput.value.trim();
    if (!listId || rows.length === 0) return;
    try {
      const res = await fetch(`/existing-words?listId=${encodeURIComponent(listId)}`);
      if (!res.ok) throw new Error((await res.json()).error || res.statusText);
      const data = await res.json();
      existingWordsSet = new Set((data.words || []).map((w) => w.trim().toLowerCase()));

      rows.forEach((row) => {
        const isDup = existingWordsSet.has(row.englishWord.trim().toLowerCase());
        row.isDuplicate = isDup;
        if (isDup) row.include = false;
      });
      render();
      const dupCount = rows.filter((r) => r.isDuplicate).length;
      showStatus(`נבדקו כפילויות מול הרשימה "${listId}": נמצאו ${dupCount} מילים קיימות (מסומנות בצהוב, לא ייכללו בהעלאה כברירת מחדל).`, 'info');
    } catch (err) {
      console.error(err);
      showStatus('שגיאה בבדיקת כפילויות: ' + err.message, 'error');
    }
  }

  refreshDupBtn.addEventListener('click', refreshDuplicates);

  selectAllBtn.addEventListener('click', () => {
    rows.forEach((r) => (r.include = true));
    render();
  });

  deselectAllBtn.addEventListener('click', () => {
    rows.forEach((r) => (r.include = false));
    render();
  });

  function render() {
    tableBody.innerHTML = '';
    emptyMsg.classList.toggle('hidden', rows.length > 0);

    rows.forEach((row) => {
      const tr = document.createElement('tr');
      if (row.isDuplicate) tr.classList.add('duplicate');
      tr.dataset.id = row.id;

      const checkboxTd = document.createElement('td');
      checkboxTd.className = 'checkbox-cell';
      const checkbox = document.createElement('input');
      checkbox.type = 'checkbox';
      checkbox.checked = row.include;
      checkbox.addEventListener('change', () => {
        row.include = checkbox.checked;
      });
      checkboxTd.appendChild(checkbox);
      tr.appendChild(checkboxTd);

      FIELDS.forEach((field) => {
        const td = document.createElement('td');
        const input = document.createElement('input');
        input.type = 'text';
        input.value = row[field];
        input.addEventListener('input', () => {
          row[field] = input.value;
        });
        td.appendChild(input);
        tr.appendChild(td);
      });

      const difficultyTd = document.createElement('td');
      const difficultyInput = document.createElement('input');
      difficultyInput.type = 'number';
      difficultyInput.min = '1';
      difficultyInput.value = row.difficulty;
      difficultyInput.addEventListener('input', () => {
        const val = Number(difficultyInput.value);
        row.difficulty = Number.isFinite(val) && val > 0 ? val : 1;
      });
      difficultyTd.appendChild(difficultyInput);
      tr.appendChild(difficultyTd);

      const tagsTd = document.createElement('td');
      const tagsInput = document.createElement('input');
      tagsInput.type = 'text';
      tagsInput.value = row.tags;
      tagsInput.addEventListener('input', () => {
        row.tags = tagsInput.value;
      });
      tagsTd.appendChild(tagsInput);
      tr.appendChild(tagsTd);

      const deleteTd = document.createElement('td');
      const deleteBtn = document.createElement('button');
      deleteBtn.textContent = 'מחק';
      deleteBtn.className = 'delete-btn';
      deleteBtn.addEventListener('click', () => {
        rows = rows.filter((r) => r.id !== row.id);
        setControlsEnabled(rows.length > 0);
        render();
      });
      deleteTd.appendChild(deleteBtn);
      tr.appendChild(deleteTd);

      tableBody.appendChild(tr);
    });
  }

  uploadBtn.addEventListener('click', async () => {
    const listId = listIdInput.value.trim();
    if (!listId) {
      showStatus('יש להזין שם רשימה.', 'error');
      return;
    }
    const toUpload = rows.filter((r) => r.include);
    if (toUpload.length === 0) {
      showStatus('לא נבחרו מילים להעלאה.', 'error');
      return;
    }

    uploadBtn.disabled = true;
    hideStatus();
    progressWrap.classList.remove('hidden');
    progressFill.style.width = '0%';
    progressText.textContent = `0 / ${toUpload.length}`;

    const CHUNK_SIZE = 50;
    let uploaded = 0;
    let failed = 0;
    let finalWordCount = null;
    const errors = [];

    for (let i = 0; i < toUpload.length; i += CHUNK_SIZE) {
      const chunk = toUpload.slice(i, i + CHUNK_SIZE).map((r) => ({
        englishWord: r.englishWord,
        partOfSpeech: r.partOfSpeech,
        hebrewTranslation: r.hebrewTranslation,
        exampleSentence: r.exampleSentence,
        hebrewExample: r.hebrewExample,
        difficulty: r.difficulty,
        tags: r.tags,
      }));

      try {
        const res = await fetch(`/upload?listId=${encodeURIComponent(listId)}`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(chunk),
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || res.statusText);
        uploaded += data.uploaded || 0;
        failed += data.failed || 0;
        if (typeof data.wordCount === 'number') finalWordCount = data.wordCount;
        if (data.errors && data.errors.length) errors.push(...data.errors);
      } catch (err) {
        console.error(err);
        failed += chunk.length;
        errors.push(err.message);
      }

      const done = Math.min(i + CHUNK_SIZE, toUpload.length);
      progressFill.style.width = `${Math.round((done / toUpload.length) * 100)}%`;
      progressText.textContent = `${done} / ${toUpload.length}`;
    }

    uploadBtn.disabled = false;

    const countSuffix = finalWordCount !== null ? ` (סה"כ ברשימה: ${finalWordCount})` : '';
    if (failed === 0) {
      showStatus(`הועלו בהצלחה ${uploaded} מילים לרשימה "${listId}".${countSuffix}`, 'success');
      rows = rows.filter((r) => !r.include);
      setControlsEnabled(rows.length > 0);
      render();
      loadLists();
    } else {
      showStatus(`הועלו ${uploaded} מילים, נכשלו ${failed}.${countSuffix} שגיאות: ${errors.slice(0, 3).join(' | ')}`, 'error');
      loadLists();
    }
  });
})();
