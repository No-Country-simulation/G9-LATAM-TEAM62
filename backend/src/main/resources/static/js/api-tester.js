// ==================== GLOBAL STATE & STORAGE ====================
let currentAuthToken = localStorage.getItem('fintech_jwt_token') || '';
let currentUserId = localStorage.getItem('fintech_user_id') || '';
let activeEndpointKey = 'authLogin';
let currentTransactionsList = [];
let selectedTransactionIds = new Set();

// Pre-fill date controls with defaults
document.addEventListener('DOMContentLoaded', () => {
    const today = new Date();
    const start30 = new Date(today);
    start30.setDate(today.getDate() - 30);

    const recEndInput = document.getElementById('recEnd');
    const recStartInput = document.getElementById('recStart');
    if (recEndInput) recEndInput.value = today.toISOString().split('T')[0];
    if (recStartInput) recStartInput.value = start30.toISOString().split('T')[0];

    const userIdInput = document.getElementById('activeUserId');
    if (currentUserId && userIdInput) {
        userIdInput.value = currentUserId;
    }

    updateAuthBadge();
    selectEndpoint('authLogin');

    // Escuchar cambios en el input global de User ID para sincronizarlo al instante
    if (userIdInput) {
        userIdInput.addEventListener('input', (e) => {
            const val = e.target.value;
            localStorage.setItem('fintech_user_id', val);
            currentUserId = val;
            updateAuthBadge();
            // Auto-rellenar campos en el formulario de endpoints
            ['epParam_id', 'epParam_userId'].forEach(id => {
                const input = document.getElementById(id);
                if (input) input.value = val;
            });
        });
    }
});

function getBaseUrl() {
    return document.getElementById('baseUrl').value.replace(/\/$/, '');
}

function updateAuthBadge() {
    const badge = document.getElementById('authBadge');
    const text = document.getElementById('authBadgeText');
    if (!badge || !text) return;

    if (currentAuthToken) {
        badge.className = 'auth-status-badge authenticated';
        text.textContent = 'JWT Activo (' + (currentUserId ? 'User ID: ' + currentUserId : 'Autenticado') + ')';
    } else {
        badge.className = 'auth-status-badge unauthenticated';
        text.textContent = 'Sin Auth (Público)';
    }
}

function setAuthData(token, userId) {
    if (token) {
        currentAuthToken = token;
        localStorage.setItem('fintech_jwt_token', token);
    }
    if (userId) {
        currentUserId = userId;
        localStorage.setItem('fintech_user_id', userId);
        const userIdInput = document.getElementById('activeUserId');
        if (userIdInput) userIdInput.value = userId;
    }
    updateAuthBadge();
}

function clearToken() {
    currentAuthToken = '';
    currentUserId = '';
    localStorage.removeItem('fintech_jwt_token');
    localStorage.removeItem('fintech_user_id');
    const userIdInput = document.getElementById('activeUserId');
    if (userIdInput) userIdInput.value = '';
    updateAuthBadge();
    alert('Token e ID de usuario limpiados.');
}

function switchTab(tabId, btn) {
    document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.tab-btn').forEach(el => el.classList.remove('active'));
    document.getElementById(tabId).classList.add('active');
    btn.classList.add('active');
}

function toggleIngestMode(mode) {
    const stmtForm = document.getElementById('formStatementMode');
    const manualForm = document.getElementById('formManualMode');
    const btnStmt = document.getElementById('btnModeStatement');
    const btnManual = document.getElementById('btnModeManual');

    if (mode === 'statement') {
        stmtForm.style.display = 'block';
        manualForm.style.display = 'none';
        btnStmt.classList.remove('btn-outline');
        btnManual.classList.add('btn-outline');
    } else {
        stmtForm.style.display = 'none';
        manualForm.style.display = 'block';
        btnManual.classList.remove('btn-outline');
        btnStmt.classList.add('btn-outline');
    }
}

// ==================== HTTP FETCH UTILITIES ====================
async function makeApiCall(url, method = 'GET', body = null, isMultipart = false, customTargetViewer = null) {
    const startTime = performance.now();
    let statusBadge = document.getElementById('responseStatus');
    let latencyBadge = document.getElementById('responseLatency');
    let viewer = document.getElementById('jsonViewer');

    if (customTargetViewer === 'ep') {
        statusBadge = document.getElementById('responseStatusEp');
        latencyBadge = document.getElementById('responseLatencyEp');
        viewer = document.getElementById('jsonViewerEp');
    } else if (customTargetViewer === 'sec') {
        statusBadge = document.getElementById('responseStatusSec');
        latencyBadge = document.getElementById('responseLatencySec');
        viewer = document.getElementById('jsonViewerSec');
    }

    viewer.innerHTML = '<span class="loader"></span> Procesando petición...';

    const headers = {};
    if (currentAuthToken) {
        headers['Authorization'] = 'Bearer ' + currentAuthToken;
    }
    if (body && !isMultipart) {
        headers['Content-Type'] = 'application/json';
    }

    const fetchOptions = {
        method: method,
        headers: headers
    };

    if (body) {
        fetchOptions.body = isMultipart ? body : (typeof body === 'string' ? body : JSON.stringify(body));
    }

    try {
        const response = await fetch(url, fetchOptions);
        const endTime = performance.now();
        const latency = Math.round(endTime - startTime);

        const statusText = `${response.status} ${response.statusText}`;
        statusBadge.textContent = statusText;
        latencyBadge.textContent = `Latencia: ${latency} ms`;

        if (response.status >= 200 && response.status < 300) {
            statusBadge.className = 'status-badge status-2xx';
        } else if (response.status >= 400 && response.status < 500) {
            statusBadge.className = 'status-badge status-4xx';
        } else {
            statusBadge.className = 'status-badge status-5xx';
        }

        let responseData;
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            responseData = await response.json();
            viewer.innerHTML = syntaxHighlightJson(responseData);
        } else {
            const rawText = await response.text();
            try {
                responseData = JSON.parse(rawText);
                viewer.innerHTML = syntaxHighlightJson(responseData);
            } catch (e) {
                viewer.textContent = rawText || '(Sin respuesta de texto)';
                responseData = rawText;
            }
        }

        return { ok: response.ok, status: response.status, data: responseData };

    } catch (error) {
        const endTime = performance.now();
        statusBadge.className = 'status-badge status-5xx';
        statusBadge.textContent = 'Error Red / CORS';
        latencyBadge.textContent = `Latencia: ${Math.round(endTime - startTime)} ms`;
        viewer.textContent = `[Error de Conexión]: ${error.message}\n\nAsegúrate de que el backend Spring Boot está corriendo en ${getBaseUrl()}`;
        return { ok: false, error: error };
    }
}

function syntaxHighlightJson(json) {
    if (typeof json !== 'string') {
        json = JSON.stringify(json, undefined, 2);
    }
    json = json.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    return json.replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g, function (match) {
        var cls = 'number';
        if (/^"/.test(match)) {
            if (/:$/.test(match)) {
                cls = 'key';
            } else {
                cls = 'string';
            }
        } else if (/true|false/.test(match)) {
            cls = 'boolean';
        } else if (/null/.test(match)) {
            cls = 'null';
        }
        return '<span class="' + cls + '">' + match + '</span>';
    });
}

function copyResponseJSON() {
    const activeTab = document.querySelector('.tab-content.active').id;
    let targetId = 'jsonViewer';
    if (activeTab === 'endpointsTab') {
        targetId = 'jsonViewerEp';
    } else if (activeTab === 'securityTab') {
        targetId = 'jsonViewerSec';
    }
    const viewer = document.getElementById(targetId);
    navigator.clipboard.writeText(viewer.textContent).then(() => {
        alert('JSON copiado al portapapeles.');
    });
}

// ==================== WORKFLOW EXECUTION FUNCTIONS ====================
async function executeWorkflowLogin() {
    const email = document.getElementById('step1Email').value;
    const password = document.getElementById('step1Password').value;

    const res = await makeApiCall(`${getBaseUrl()}/api/auth/login`, 'POST', { email, password });
    if (res.ok && res.data.token) {
        setAuthData(res.data.token, res.data.user ? res.data.user.id : '');
        document.getElementById('step1Card').classList.remove('active');
        document.getElementById('step2Card').classList.add('active');
    }
}

async function executeWorkflowRegister() {
    const email = document.getElementById('step1Email').value;
    const password = document.getElementById('step1Password').value;
    const name = document.getElementById('step1Name').value;
    const monthlyIncome = parseFloat(document.getElementById('step1Income').value) || 0;

    const res = await makeApiCall(`${getBaseUrl()}/api/auth/register`, 'POST', {
        name,
        email,
        password,
        monthlyIncome,
        savingFrequency: 'MONTHLY'
    });

    if (res.ok && res.data.id) {
        alert(`Usuario registrado con éxito (ID: ${res.data.id}). Procediendo al Login automático...`);
        await executeWorkflowLogin();
    }
}

async function executeWorkflowUploadStatement() {
    const userId = document.getElementById('activeUserId').value || currentUserId;
    if (!userId) {
        alert('Por favor especifica un User ID o inicia sesión primero.');
        return;
    }
    const fileInput = document.getElementById('statementFile');
    if (!fileInput.files || fileInput.files.length === 0) {
        alert('Selecciona un archivo de cartola (Excel, CSV o PDF).');
        return;
    }

    const formData = new FormData();
    formData.append('file', fileInput.files[0]);
    formData.append('userId', userId);
    formData.append('defaultYear', document.getElementById('statementYear').value || '2026');
    formData.append('country', document.getElementById('statementCountry').value || 'CL');

    const res = await makeApiCall(`${getBaseUrl()}/api/transactions/upload-statement`, 'POST', formData, true);
    if (res.ok) {
        document.getElementById('step2Card').classList.remove('active');
        document.getElementById('step3Card').classList.add('active');
        await executeWorkflowFetchTransactions();
    }
}

async function executeWorkflowCreateManualTx() {
    const userId = document.getElementById('activeUserId').value || currentUserId;
    if (!userId) {
        alert('Especifica un User ID o inicia sesión primero.');
        return;
    }

    const currencyName = document.getElementById('manualCurrency').value;
    const opNum = document.getElementById('manualOpNumber').value.trim();

    const payload = {
        userId: parseInt(userId),
        amount: parseFloat(document.getElementById('manualAmount').value),
        category: document.getElementById('manualCategory').value,
        description: document.getElementById('manualDesc').value,
        currency: { name_currency: currencyName },
        paymentMethod: document.getElementById('manualMethod').value,
        bankName: document.getElementById('manualBankName').value,
        operationNumber: opNum || null
    };

    const res = await makeApiCall(`${getBaseUrl()}/api/transactions/manual`, 'POST', payload);
    if (res.ok) {
        document.getElementById('step2Card').classList.remove('active');
        document.getElementById('step3Card').classList.add('active');
        await executeWorkflowFetchTransactions();
    }
}

async function executeWorkflowFetchTransactions() {
    const userId = document.getElementById('activeUserId').value || currentUserId;
    const url = userId ? `${getBaseUrl()}/api/transactions?userId=${userId}` : `${getBaseUrl()}/api/transactions`;

    const res = await makeApiCall(url, 'GET');
    if (res.ok && Array.isArray(res.data)) {
        currentTransactionsList = res.data;
        const validIds = new Set(res.data.map(t => t.id));
        selectedTransactionIds = new Set([...selectedTransactionIds].filter(id => validIds.has(id)));
        filterAndRenderTransactions();
    }
}

function filterAndRenderTransactions() {
    const container = document.getElementById('txListContainer');
    if (!container) return;

    if (!currentTransactionsList || currentTransactionsList.length === 0) {
        container.innerHTML = '<p style="font-size: 13px; color: var(--text-dim); text-align: center; padding: 12px;">No se encontraron transacciones para este usuario. Sube una cartola en el Paso 2 o crea una manual.</p>';
        updateSelectedSummary();
        return;
    }

    const searchQuery = (document.getElementById('txSearchInput')?.value || '').toLowerCase().trim();
    const categoryFilter = document.getElementById('txCategoryFilter')?.value || 'ALL';
    const methodFilter = document.getElementById('txMethodFilter')?.value || 'ALL';
    const sourceFilter = document.getElementById('txSourceFilter')?.value || 'ALL';
    const sortBy = document.getElementById('txSortBy')?.value || 'date-desc';

    // 1. Filtrado
    let filtered = currentTransactionsList.filter(tx => {
        if (searchQuery) {
            const desc = (tx.description || '').toLowerCase();
            const opNum = (tx.operationNumber || '').toString().toLowerCase();
            const idStr = (tx.id || '').toString();
            const bank = (tx.bankName || '').toLowerCase();
            if (!desc.includes(searchQuery) && !opNum.includes(searchQuery) && !idStr.includes(searchQuery) && !bank.includes(searchQuery)) {
                return false;
            }
        }
        if (categoryFilter !== 'ALL' && (tx.category || '') !== categoryFilter) return false;
        if (methodFilter !== 'ALL' && (tx.paymentMethod || '') !== methodFilter) return false;
        if (sourceFilter !== 'ALL' && (tx.source || '') !== sourceFilter) return false;
        return true;
    });

    // 2. Ordenamiento
    filtered.sort((a, b) => {
        switch (sortBy) {
            case 'date-desc':
                return new Date(b.date || '1970-01-01') - new Date(a.date || '1970-01-01') || (b.id - a.id);
            case 'date-asc':
                return new Date(a.date || '1970-01-01') - new Date(b.date || '1970-01-01') || (a.id - b.id);
            case 'amount-desc':
                return (b.amount || 0) - (a.amount || 0);
            case 'amount-asc':
                return (a.amount || 0) - (b.amount || 0);
            case 'desc-asc':
                return (a.description || '').localeCompare(b.description || '');
            case 'cat-asc':
                return (a.category || '').localeCompare(b.category || '');
            default:
                return b.id - a.id;
        }
    });

    renderTransactionsTable(filtered);
    updateSelectedSummary();
}

function renderTransactionsTable(transactions) {
    const container = document.getElementById('txListContainer');
    if (!container) return;

    if (transactions.length === 0) {
        container.innerHTML = '<p style="font-size: 13px; color: var(--text-dim); text-align: center; padding: 12px;">Ninguna transacción coincide con los filtros aplicados.</p>';
        return;
    }

    const allFilteredSelected = transactions.length > 0 && transactions.every(tx => selectedTransactionIds.has(tx.id));

    let html = `
        <table class="data-table">
            <thead>
                <tr>
                    <th style="width: 36px; text-align: center;">
                        <input type="checkbox" id="selectAllCheckbox" onchange="toggleSelectFilteredTransactions(this.checked)" ${allFilteredSelected ? 'checked' : ''} title="Seleccionar/Deseleccionar mostradas">
                    </th>
                    <th>ID</th>
                    <th>Fecha</th>
                    <th>Descripción</th>
                    <th>N° Operación</th>
                    <th>Monto</th>
                    <th>Origen / Tipo</th>
                    <th>Categoría Actual</th>
                    <th>Método Clasif.</th>
                    <th>Acción (Feedback)</th>
                </tr>
            </thead>
            <tbody>
    `;

    transactions.forEach(tx => {
        const isChecked = selectedTransactionIds.has(tx.id);
        const currencyCode = (tx.currency && tx.currency.name_currency) ? tx.currency.name_currency : (tx.currency && tx.currency.nameCurrency ? tx.currency.nameCurrency : 'CLP');
        const sourceBadge = tx.source === 'BANK' ? '<span class="badge-chip badge-bank">BANK</span>' : '<span class="badge-chip badge-manual">MANUAL</span>';
        const methodText = tx.paymentMethod || 'DEBIT';

        html += `
            <tr style="${isChecked ? 'background: rgba(56, 139, 253, 0.08);' : ''}">
                <td style="text-align: center;">
                    <input type="checkbox" onchange="toggleTransactionSelection(${tx.id}, this.checked)" ${isChecked ? 'checked' : ''}>
                </td>
                <td><small style="color: var(--text-muted);">#${tx.id}</small></td>
                <td>${tx.date || 'N/A'}</td>
                <td><strong>${escapeHtml(tx.description || 'Sin descripción')}</strong></td>
                <td><code style="font-size: 11px; color: #79c0ff;">${tx.operationNumber ? escapeHtml(tx.operationNumber) : '<span style="color:var(--text-dim);">—</span>'}</code></td>
                <td><strong>$${(tx.amount || 0).toLocaleString()}</strong> <small style="color: var(--text-dim);">${currencyCode}</small></td>
                <td>${sourceBadge} <small style="color: var(--text-muted);">${methodText}</small></td>
                <td><span class="badge-chip badge-category">${tx.category || 'N/A'}</span></td>
                <td><small style="color: var(--text-muted);">${tx.categoryMethod || 'MANUAL'}</small></td>
                <td>
                    <select id="corrCat_${tx.id}" style="padding: 3px 6px; font-size: 11px; width: 110px;">
                        <option value="FOOD" ${tx.category === 'FOOD' ? 'selected' : ''}>FOOD</option>
                        <option value="TRANSPORT" ${tx.category === 'TRANSPORT' ? 'selected' : ''}>TRANSPORT</option>
                        <option value="HOUSING" ${tx.category === 'HOUSING' ? 'selected' : ''}>HOUSING</option>
                        <option value="UTILITIES" ${tx.category === 'UTILITIES' ? 'selected' : ''}>UTILITIES</option>
                        <option value="ENTERTAINMENT" ${tx.category === 'ENTERTAINMENT' ? 'selected' : ''}>ENTERTAINMENT</option>
                        <option value="HEALTH" ${tx.category === 'HEALTH' ? 'selected' : ''}>HEALTH</option>
                        <option value="EDUCATION" ${tx.category === 'EDUCATION' ? 'selected' : ''}>EDUCATION</option>
                        <option value="SHOPPING" ${tx.category === 'SHOPPING' ? 'selected' : ''}>SHOPPING</option>
                        <option value="SALARY" ${tx.category === 'SALARY' ? 'selected' : ''}>SALARY</option>
                        <option value="OTHER_EXPENSE" ${tx.category === 'OTHER_EXPENSE' ? 'selected' : ''}>OTHER_EXPENSE</option>
                        <option value="OTHER_INCOME" ${tx.category === 'OTHER_INCOME' ? 'selected' : ''}>OTHER_INCOME</option>
                    </select>
                    <button class="btn btn-outline btn-sm" onclick="executeCategoryCorrection(${tx.id})" style="padding: 2px 6px; font-size: 11px;">Corregir</button>
                </td>
            </tr>
        `;
    });

    html += `</tbody></table>`;
    container.innerHTML = html;
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function toggleTransactionSelection(txId, isSelected) {
    if (isSelected) {
        selectedTransactionIds.add(txId);
    } else {
        selectedTransactionIds.delete(txId);
    }
    filterAndRenderTransactions();
}

function toggleSelectAllTransactions(selectAll) {
    if (selectAll) {
        currentTransactionsList.forEach(tx => selectedTransactionIds.add(tx.id));
    } else {
        selectedTransactionIds.clear();
    }
    filterAndRenderTransactions();
}

function toggleSelectFilteredTransactions(selectAll) {
    const searchQuery = (document.getElementById('txSearchInput')?.value || '').toLowerCase().trim();
    const categoryFilter = document.getElementById('txCategoryFilter')?.value || 'ALL';
    const methodFilter = document.getElementById('txMethodFilter')?.value || 'ALL';
    const sourceFilter = document.getElementById('txSourceFilter')?.value || 'ALL';

    currentTransactionsList.forEach(tx => {
        let matches = true;
        if (searchQuery) {
            const desc = (tx.description || '').toLowerCase();
            const opNum = (tx.operationNumber || '').toString().toLowerCase();
            const idStr = (tx.id || '').toString();
            const bank = (tx.bankName || '').toLowerCase();
            if (!desc.includes(searchQuery) && !opNum.includes(searchQuery) && !idStr.includes(searchQuery) && !bank.includes(searchQuery)) matches = false;
        }
        if (categoryFilter !== 'ALL' && (tx.category || '') !== categoryFilter) matches = false;
        if (methodFilter !== 'ALL' && (tx.paymentMethod || '') !== methodFilter) matches = false;
        if (sourceFilter !== 'ALL' && (tx.source || '') !== sourceFilter) matches = false;

        if (matches) {
            if (selectAll) selectedTransactionIds.add(tx.id);
            else selectedTransactionIds.delete(tx.id);
        }
    });
    filterAndRenderTransactions();
}

function updateSelectedSummary() {
    const badge = document.getElementById('selectedTxBadge');
    const summary = document.getElementById('selectedSummaryText');
    const dbSelectedCount = document.getElementById('analysisDbSelectedCount');
    const dbSummaryBox = document.getElementById('analysisDbSummaryBox');

    const totalCount = currentTransactionsList.length;
    const selectedCount = selectedTransactionIds.size;

    let totalSelectedAmount = 0;
    let expenseSum = 0;
    let incomeSum = 0;

    currentTransactionsList.forEach(tx => {
        if (selectedTransactionIds.has(tx.id)) {
            const val = Math.abs(tx.amount || 0);
            totalSelectedAmount += val;
            if (tx.type === 'INCOME' || tx.category === 'SALARY' || tx.category === 'OTHER_INCOME') {
                incomeSum += val;
            } else {
                expenseSum += val;
            }
        }
    });

    if (badge) badge.textContent = `${selectedCount} seleccionadas (de ${totalCount})`;
    if (summary) {
        summary.innerHTML = `Monto Seleccionado: <strong>$${totalSelectedAmount.toLocaleString()}</strong> (Gastos: $${expenseSum.toLocaleString()} | Ingresos: $${incomeSum.toLocaleString()})`;
    }
    if (dbSelectedCount) dbSelectedCount.textContent = selectedCount;
    if (dbSummaryBox) {
        const isSelectedScope = document.getElementById('scopeSelected')?.checked;
        if (isSelectedScope) {
            dbSummaryBox.innerHTML = `Se analizarán <strong>${selectedCount}</strong> transacciones seleccionadas (Gasto total estimado: <strong>$${expenseSum.toLocaleString()}</strong>).`;
        } else {
            dbSummaryBox.innerHTML = `Se analizarán <strong>todas (${totalCount})</strong> las transacciones de tu cuenta registradas en la BD.`;
        }
    }
}

function sendSelectedToAnalysis() {
    if (selectedTransactionIds.size === 0) {
        alert('Por favor selecciona al menos 1 transacción o selecciona la opción "Todas las transacciones" en el Paso 5.');
    }
    const scopeSelected = document.getElementById('scopeSelected');
    if (scopeSelected) scopeSelected.checked = true;
    updateAnalysisDbScope();

    const step5 = document.getElementById('step5Card');
    if (step5) {
        step5.classList.add('active');
        step5.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
}

async function executeCategoryCorrection(txId) {
    const newCategory = document.getElementById(`corrCat_${txId}`).value;
    const res = await makeApiCall(`${getBaseUrl()}/api/transactions/${txId}/category`, 'PUT', { category: newCategory });
    if (res.ok) {
        alert(`Categoría de transacción #${txId} actualizada a ${newCategory} (Feedback guardado).`);
        await executeWorkflowFetchTransactions();
    }
}

async function executeWorkflowGenerateRecommendations() {
    const userId = document.getElementById('activeUserId').value || currentUserId;
    if (!userId) {
        alert('Especifique el User ID primero.');
        return;
    }

    const start = document.getElementById('recStart').value;
    const end = document.getElementById('recEnd').value;

    let url = `${getBaseUrl()}/api/recommendations/generate?userId=${userId}`;
    if (start) url += `&periodStart=${start}`;
    if (end) url += `&periodEnd=${end}`;

    const res = await makeApiCall(url, 'POST');
    if (res.ok && Array.isArray(res.data)) {
        renderRecommendations(res.data);
    }
}

function renderRecommendations(list) {
    const container = document.getElementById('recommendationsOutput');
    if (!container) return;

    if (list.length === 0) {
        container.innerHTML = '<div class="recommendation-card OPTIMAL"><div class="rec-header">🎉 ¡Presupuesto Óptimo!</div><div class="rec-body">No se detectaron desviaciones significativas en tus gastos en comparación con las referencias INE Chile.</div></div>';
        return;
    }

    let html = '';
    list.forEach(rec => {
        const statusClass = rec.status || 'WARNING';
        const displayText = rec.text || rec.message || 'Sin mensaje especificado.';
        const headerText = rec.category || 'Alerta de Gasto';

        let spendHtml = '';
        if (rec.currentSpend !== undefined && rec.recommendedLimit !== undefined) {
            spendHtml = `<span style="font-size: 12px; opacity: 0.8;">Gasto: $${rec.currentSpend.toLocaleString()} vs Límite: $${rec.recommendedLimit.toLocaleString()}</span>`;
        } else if (rec.generatedAt) {
            spendHtml = `<span style="font-size: 12px; opacity: 0.8;">Generada: ${new Date(rec.generatedAt).toLocaleDateString()}</span>`;
        }

        html += `
            <div class="recommendation-card ${statusClass}">
                <div class="rec-header">
                    <span>${headerText}</span>
                    ${spendHtml}
                </div>
                <div class="rec-body">${displayText}</div>
            </div>
        `;
    });

    container.innerHTML = html;
}

// ==================== ENDPOINT TESTER CONFIGURATION ====================
const endpointConfigs = {
    authLogin: {
        title: 'POST /api/auth/login',
        method: 'POST',
        path: '/api/auth/login',
        hasBody: true,
        hasFile: false,
        preset: { email: 'juan.perez@example.com', password: 'Password123!' }
    },
    authRegister: {
        title: 'POST /api/auth/register',
        method: 'POST',
        path: '/api/auth/register',
        hasBody: true,
        hasFile: false,
        preset: {
            name: 'Juan Pérez',
            email: 'juan.perez@example.com',
            password: 'Password123!',
            monthlyIncome: 1200000.00,
            savingFrequency: 'MONTHLY'
        }
    },
    usersGetAll: {
        title: 'GET /api/users',
        method: 'GET',
        path: '/api/users',
        hasBody: false,
        hasFile: false
    },
    usersGetById: {
        title: 'GET /api/users/{id}',
        method: 'GET',
        path: '/api/users/{id}',
        params: [{ name: 'id', label: 'User ID', default: '1' }],
        hasBody: false,
        hasFile: false
    },
    usersUpdate: {
        title: 'PUT /api/users/{id}',
        method: 'PUT',
        path: '/api/users/{id}',
        params: [{ name: 'id', label: 'User ID', default: '1' }],
        hasBody: true,
        hasFile: false,
        preset: {
            name: 'Juan Pérez Actualizado',
            email: 'juan.perez@example.com',
            monthlyIncome: 1500000.00,
            savingFrequency: 'BIWEEKLY'
        }
    },
    usersUpdateProfile: {
        title: 'PUT /api/users/{id}/profile',
        method: 'PUT',
        path: '/api/users/{id}/profile',
        params: [{ name: 'id', label: 'User ID', default: '1' }],
        hasBody: true,
        hasFile: false,
        preset: {
            financialProfile: 'BALANCED',
            profileAccuracy: 0.85,
            savingFrequency: 'MONTHLY'
        }
    },
    usersChangePassword: {
        title: 'POST /api/users/change-password',
        method: 'POST',
        path: '/api/users/change-password',
        hasBody: true,
        hasFile: false,
        preset: { oldPassword: 'Password123!', newPassword: 'NewPassword456!' }
    },
    txGetAll: {
        title: 'GET /api/transactions',
        method: 'GET',
        path: '/api/transactions',
        queryParams: [{ name: 'userId', label: 'Filtro User ID (Opcional)', default: '1' }],
        hasBody: false,
        hasFile: false
    },
    txCreate: {
        title: 'POST /api/transactions',
        method: 'POST',
        path: '/api/transactions',
        hasBody: true,
        hasFile: false,
        preset: {
            description: 'COMPRA SUPERMERCADO LIDER',
            amount: 45990.00,
            category: 'FOOD',
            date: '2026-08-01',
            currency: { name_currency: 'CLP' },
            userId: 1,
            paymentMethod: 'DEBIT',
            bankName: 'Banco de Chile',
            operationNumber: 'OP-TEST-777'
        }
    },
    txCreateManual: {
        title: 'POST /api/transactions/manual',
        method: 'POST',
        path: '/api/transactions/manual',
        hasBody: true,
        hasFile: false,
        preset: {
            userId: 1,
            amount: 25000.00,
            category: 'TRANSPORT',
            description: 'CARGA BIP METRO',
            currency: { name_currency: 'CLP' },
            paymentMethod: 'CASH',
            bankName: 'Efectivo',
            operationNumber: 'OP-MANUAL-888'
        }
    },
    txUploadStatement: {
        title: 'POST /api/transactions/upload-statement',
        method: 'POST',
        path: '/api/transactions/upload-statement',
        queryParams: [
            { name: 'userId', label: 'User ID', default: '1' },
            { name: 'defaultYear', label: 'Año (Opcional)', default: '2026' },
            { name: 'country', label: 'País (Opcional)', default: 'CL' }
        ],
        hasBody: false,
        hasFile: true
    },
    txUpdateCategory: {
        title: 'PUT /api/transactions/{id}/category',
        method: 'PUT',
        path: '/api/transactions/{id}/category',
        params: [{ name: 'id', label: 'Transaction ID', default: '1' }],
        hasBody: true,
        hasFile: false,
        preset: { category: 'ENTERTAINMENT' }
    },
    recGenerate: {
        title: 'POST /api/recommendations/generate',
        method: 'POST',
        path: '/api/recommendations/generate',
        queryParams: [
            { name: 'userId', label: 'User ID', default: '1' },
            { name: 'periodStart', label: 'Fecha Inicio (YYYY-MM-DD)', default: '2026-07-01' },
            { name: 'periodEnd', label: 'Fecha Fin (YYYY-MM-DD)', default: '2026-08-09' }
        ],
        hasBody: false,
        hasFile: false
    },
    analisisFinanciero: {
        title: 'POST /api/analisis-financiero',
        method: 'POST',
        path: '/api/analisis-financiero',
        hasBody: true,
        hasFile: false,
        preset: {
            ingreso_mensual: 1500000.00,
            nivel_endeudamiento: 12.5,
            frecuencia_ahorro: 'Media',
            transacciones: [
                { descripcion: 'COMPRA SUPERMERCADO JUMBO', valor: 78990.00 },
                { descripcion: 'PAGO UBER VIAJE', valor: 6500.00 },
                { descripcion: 'PAGO DEPARTAMENTO VIVIENDA', valor: 320000.00 }
            ]
        }
    },
    usersGetProfileHistory: {
        title: 'GET /api/users/{id}/profile-history',
        method: 'GET',
        path: '/api/users/{id}/profile-history',
        params: [{ name: 'id', label: 'User ID', default: '1' }],
        hasBody: false,
        hasFile: false
    },
    recommendationsGetHistory: {
        title: 'GET /api/recommendations/user/{userId}',
        method: 'GET',
        path: '/api/recommendations/user/{userId}',
        params: [{ name: 'userId', label: 'User ID', default: '1' }],
        hasBody: false,
        hasFile: false
    }
};

function selectEndpoint(key) {
    activeEndpointKey = key;
    const config = endpointConfigs[key];

    document.querySelectorAll('.endpoint-item').forEach(el => el.classList.remove('active'));
    
    // Add active styling to selected list item
    const items = document.querySelectorAll('.endpoint-item');
    items.forEach(item => {
        if (item.getAttribute('onclick') && item.getAttribute('onclick').includes(`'${key}'`)) {
            item.classList.add('active');
        }
    });

    const pathText = document.getElementById('epPathText');
    if (pathText) pathText.textContent = config.path;
    
    const badge = document.getElementById('epMethodBadge');
    if (badge) {
        badge.textContent = config.method;
        badge.className = `method-badge method-${config.method.toLowerCase()}`;
    }

    // Render path / query params
    const paramsContainer = document.getElementById('epParamsContainer');
    if (paramsContainer) {
        paramsContainer.innerHTML = '';
        const allParams = [...(config.params || []), ...(config.queryParams || [])];
        if (allParams.length > 0) {
            let html = '<div class="form-grid">';
            allParams.forEach(p => {
                let defaultValue = p.default || '';
                // Sincronizar automáticamente con el User ID global si aplica
                if ((p.name === 'id' || p.name === 'userId') && document.getElementById('activeUserId').value) {
                    defaultValue = document.getElementById('activeUserId').value;
                }
                html += `
                    <div class="form-group">
                        <label for="epParam_${p.name}">${p.label}</label>
                        <input type="text" id="epParam_${p.name}" value="${defaultValue}">
                    </div>
                `;
            });
            html += '</div>';
            paramsContainer.innerHTML = html;
        }
    }

    // Body group
    const bodyGroup = document.getElementById('epBodyGroup');
    if (bodyGroup) {
        if (config.hasBody) {
            bodyGroup.style.display = 'block';
            document.getElementById('epRequestBody').value = JSON.stringify(config.preset || {}, null, 2);
        } else {
            bodyGroup.style.display = 'none';
        }
    }

    // File group
    const fileGroup = document.getElementById('epFileGroup');
    if (fileGroup) {
        fileGroup.style.display = config.hasFile ? 'block' : 'none';
    }
}

function loadEndpointPreset() {
    const config = endpointConfigs[activeEndpointKey];
    if (config && config.preset) {
        document.getElementById('epRequestBody').value = JSON.stringify(config.preset, null, 2);
    }
}

async function executeSelectedEndpoint() {
    const config = endpointConfigs[activeEndpointKey];
    let url = getBaseUrl() + config.path;

    // Substitute path parameters
    if (config.params) {
        config.params.forEach(p => {
            const val = document.getElementById(`epParam_${p.name}`).value;
            url = url.replace(`{${p.name}}`, encodeURIComponent(val));
        });
    }

    // Append query parameters
    if (config.queryParams) {
        const queryParts = [];
        config.queryParams.forEach(p => {
            const val = document.getElementById(`epParam_${p.name}`)?.value;
            if (val) queryParts.push(`${p.name}=${encodeURIComponent(val)}`);
        });
        if (queryParts.length > 0) {
            url += '?' + queryParts.join('&');
        }
    }

    if (config.hasFile) {
        const fileInput = document.getElementById('epFileInput');
        if (!fileInput.files || fileInput.files.length === 0) {
            alert('Por favor selecciona un archivo primero.');
            return;
        }
        const formData = new FormData();
        formData.append('file', fileInput.files[0]);

        // Agregar también los parámetros como campos del FormData para máxima compatibilidad con el servidor
        if (config.queryParams) {
            config.queryParams.forEach(p => {
                const val = document.getElementById(`epParam_${p.name}`)?.value;
                if (val) formData.append(p.name, val);
            });
        }

        await makeApiCall(url, config.method, formData, true, 'ep');
    } else if (config.hasBody) {
        const bodyText = document.getElementById('epRequestBody').value;
        await makeApiCall(url, config.method, bodyText, false, 'ep');
    } else {
        await makeApiCall(url, config.method, null, false, 'ep');
    }
}

// ==================== SECURITY TESTER FUNCTIONS ====================
async function testIdorFetchProfile() {
    const victimId = document.getElementById('secVictimUserId').value || '999';
    await makeApiCall(`${getBaseUrl()}/api/users/${victimId}`, 'GET', null, false, 'sec');
}

async function testIdorUpdateProfile() {
    const victimId = document.getElementById('secVictimUserId').value || '999';
    const payload = {
        financialProfile: 'BALANCED',
        profileAccuracy: 0.99,
        savingFrequency: 'MONTHLY'
    };
    await makeApiCall(`${getBaseUrl()}/api/users/${victimId}/profile`, 'PUT', payload, false, 'sec');
}

async function testIdorFetchTransaction() {
    const victimTxId = document.getElementById('secVictimTxId').value || '999';
    await makeApiCall(`${getBaseUrl()}/api/transactions/${victimTxId}`, 'GET', null, false, 'sec');
}

async function testIdorDeleteTransaction() {
    const victimTxId = document.getElementById('secVictimTxId').value || '999';
    await makeApiCall(`${getBaseUrl()}/api/transactions/${victimTxId}`, 'DELETE', null, false, 'sec');
}

async function checkCorsHeaders() {
    const startTime = performance.now();
    const statusBadge = document.getElementById('responseStatusSec');
    const latencyBadge = document.getElementById('responseLatencySec');
    const viewer = document.getElementById('jsonViewerSec');

    viewer.innerHTML = '<span class="loader"></span> Simulando OPTIONS preflight...';

    try {
        const response = await fetch(`${getBaseUrl()}/api/auth/login`, {
            method: 'OPTIONS',
            headers: {
                'Access-Control-Request-Method': 'POST',
                'Access-Control-Request-Headers': 'content-type',
                'Origin': 'http://127.0.0.1:5500'
            }
        });

        const endTime = performance.now();
        const latency = Math.round(endTime - startTime);

        statusBadge.textContent = `${response.status} ${response.statusText}`;
        latencyBadge.textContent = `Latencia: ${latency} ms`;
        statusBadge.className = response.ok ? 'status-badge status-2xx' : 'status-badge status-4xx';

        const headersObj = {};
        response.headers.forEach((value, key) => {
            headersObj[key] = value;
        });

        viewer.innerHTML = `<strong>Cabeceras de Respuesta CORS en OPTIONS:</strong>\n\n` + syntaxHighlightJson(headersObj);
    } catch (error) {
        const endTime = performance.now();
        statusBadge.className = 'status-badge status-5xx';
        statusBadge.textContent = 'Error CORS / Red';
        latencyBadge.textContent = `Latencia: ${Math.round(endTime - startTime)} ms`;
        viewer.textContent = `[Error]: ${error.message}\n\nSi el navegador bloqueó la petición por política de mismo origen, el CORS está funcionando correctamente en el backend.`;
    }
}

function updateAnalysisDbScope() {
    updateSelectedSummary();
}

async function syncUserProfileToAnalysis() {
    const userId = document.getElementById('activeUserId').value || currentUserId;
    if (!userId) {
        alert('Ingresa tu User ID primero.');
        return;
    }
    const res = await makeApiCall(`${getBaseUrl()}/api/users/${userId}`, 'GET');
    if (res.ok && res.data) {
        if (res.data.monthlyIncome || res.data.monthly_income) {
            document.getElementById('analysisIncome').value = res.data.monthlyIncome || res.data.monthly_income;
        }
        if (res.data.savingFrequency || res.data.saving_frequency) {
            const freq = res.data.savingFrequency || res.data.saving_frequency;
            const select = document.getElementById('analysisSavingFreq');
            if (select) {
                for (let i = 0; i < select.options.length; i++) {
                    if (select.options[i].value.toUpperCase() === freq.toUpperCase() || select.options[i].text.toUpperCase().includes(freq.toUpperCase())) {
                        select.selectedIndex = i;
                        break;
                    }
                }
            }
        }
        alert('Datos de perfil sincronizados: Ingreso Mensual $' + Number(document.getElementById('analysisIncome').value).toLocaleString());
    }
}

async function executeWorkflowFinancialAnalysis() {
    const income = parseFloat(document.getElementById('analysisIncome').value) || 0;
    const debt = parseFloat(document.getElementById('analysisDebt').value) || 0;
    const savingFreq = document.getElementById('analysisSavingFreq').value;

    let payload = {
        ingreso_mensual: income,
        nivel_endeudamiento: debt,
        frecuencia_ahorro: savingFreq
    };

    const useAll = document.getElementById('scopeAll')?.checked;
    if (!useAll) {
        if (selectedTransactionIds.size === 0) {
            alert('No has seleccionado transacciones en el Paso 3. Puedes marcar transacciones en la tabla o elegir "Usar todas las transacciones de mi cuenta".');
            return;
        }
        payload.transaction_ids = Array.from(selectedTransactionIds);
    } else {
        payload.transaction_ids = [];
    }

    const res = await makeApiCall(`${getBaseUrl()}/api/analisis-financiero`, 'POST', payload);
    if (res.ok && res.data) {
        renderFinancialAnalysisResults(res.data, income, debt);
        document.getElementById('step5Card').classList.add('active');
        document.getElementById('step6Card').classList.add('active');
        // Auto refresh recommendations & profile history in Step 6
        executeWorkflowFetchProfileHistory();
        executeWorkflowFetchRecommendationHistory();
    }
}

function renderFinancialAnalysisResults(data, income, debt) {
    const container = document.getElementById('financialAnalysisResultContainer');
    if (!container) return;

    const perfil = data.perfil_financiero || 'Saludable';
    const accuracy = data.precision ? (data.precision * 100).toFixed(0) : '90';
    const resumen = data.resumen_gastos || {};
    const recs = data.recomendaciones || [];

    const perfilClass = perfil.replace(/\s+/g, '-');
    const badgeColor = perfil === 'Saludable' ? '#2ea043' : (perfil === 'En riesgo' ? '#f85149' : '#d29922');
    const badgeIcon = perfil === 'Saludable' ? '🌟' : (perfil === 'En riesgo' ? '⚠️' : '⚖️');

    let totalGastos = 0;
    Object.values(resumen).forEach(v => totalGastos += (Number(v) || 0));

    let gastosRowsHtml = '';
    const entries = Object.entries(resumen);
    if (entries.length === 0) {
        gastosRowsHtml = '<tr><td colspan="3" style="text-align: center; color: var(--text-dim);">No se registraron gastos en las transacciones analizadas.</td></tr>';
    } else {
        entries.forEach(([cat, val]) => {
            const amount = Number(val) || 0;
            const pctOfIncome = income > 0 ? ((amount / income) * 100).toFixed(1) : '—';
            const pctOfTotal = totalGastos > 0 ? ((amount / totalGastos) * 100).toFixed(1) : '0';
            gastosRowsHtml += `
                <tr>
                    <td><strong>${escapeHtml(cat.toUpperCase())}</strong></td>
                    <td>$${amount.toLocaleString()} <small style="color: var(--text-muted);">(${pctOfTotal}% del gasto)</small></td>
                    <td style="width: 35%;">
                        <div style="display: flex; align-items: center; justify-content: space-between; font-size: 11px;">
                            <span>${pctOfIncome}% del ingreso</span>
                        </div>
                        <div class="expense-progress-bar">
                            <div class="expense-progress-fill" style="width: ${Math.min(Number(pctOfTotal), 100)}%;"></div>
                        </div>
                    </td>
                </tr>
            `;
        });
    }

    let recsHtml = '';
    if (recs.length === 0) {
        recsHtml = '<p style="color: var(--text-muted); font-size: 13px;">No se encontraron observaciones presupuestarias adicionales.</p>';
    } else {
        recs.forEach(rec => {
            recsHtml += `
                <div class="rec-pill">
                    <span>💡</span>
                    <div>${escapeHtml(rec)}</div>
                </div>
            `;
        });
    }

    container.innerHTML = `
        <div class="analysis-result-box">
            <div class="profile-hero ${perfilClass}">
                <div>
                    <div style="font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px; color: var(--text-muted);">Perfil Financiero Determinado</div>
                    <div style="font-size: 22px; font-weight: 700; color: ${badgeColor}; display: flex; align-items: center; gap: 8px;">
                        ${badgeIcon} ${escapeHtml(perfil)}
                    </div>
                </div>
                <div style="text-align: right;">
                    <span class="badge-chip" style="background: rgba(255,255,255,0.1); font-size: 12px;">Precisión: ${accuracy}%</span>
                    <div style="font-size: 11px; color: var(--text-muted); margin-top: 4px;">Endeudamiento: ${debt}%</div>
                </div>
            </div>

            <h4 style="margin-bottom: 8px; font-size: 14px;">📊 Resumen de Gastos por Categoría (Total: $${totalGastos.toLocaleString()}):</h4>
            <table class="data-table" style="margin-bottom: 16px;">
                <thead>
                    <tr>
                        <th>Categoría</th>
                        <th>Monto Total</th>
                        <th>Proporción / Presupuesto</th>
                    </tr>
                </thead>
                <tbody>
                    ${gastosRowsHtml}
                </tbody>
            </table>

            <h4 style="margin-bottom: 8px; font-size: 14px;">📑 Recomendaciones y Diagnóstico Presupuestario (INE Chile):</h4>
            <div style="margin-top: 6px;">
                ${recsHtml}
            </div>
        </div>
    `;
    container.style.display = 'block';
}

async function executeWorkflowFetchProfileHistory() {
    const userId = document.getElementById('activeUserId').value || currentUserId;
    if (!userId) {
        alert('Especifica un User ID o inicia sesión primero.');
        return;
    }

    const res = await makeApiCall(`${getBaseUrl()}/api/users/${userId}/profile-history`, 'GET');
    if (res.ok && Array.isArray(res.data)) {
        let html = '<h4>Historial de Perfiles Financieros:</h4><ul style="font-size:13px; padding-left:20px; color:var(--text-muted);">';
        res.data.forEach(h => {
            html += `<li><strong>Perfil:</strong> ${h.financialProfile} | <strong>Precisión:</strong> ${(h.profileAccuracy * 100).toFixed(1)}% | <strong>Fecha:</strong> ${new Date(h.createdAt).toLocaleString()}</li>`;
        });
        html += '</ul>';
        document.getElementById('historyResultOutput').innerHTML = html;
    }
}

async function executeWorkflowFetchRecommendationHistory() {
    const userId = document.getElementById('activeUserId').value || currentUserId;
    if (!userId) {
        alert('Especifica un User ID o inicia sesión primero.');
        return;
    }

    const res = await makeApiCall(`${getBaseUrl()}/api/recommendations/user/${userId}`, 'GET');
    if (res.ok && Array.isArray(res.data)) {
        let html = '<h4>Historial de Recomendaciones Guardadas:</h4><ul style="font-size:13px; padding-left:20px; color:var(--text-muted);">';
        res.data.forEach(r => {
            html += `<li><strong>Recomendación:</strong> ${r.text} | <strong>Fecha:</strong> ${r.generatedAt ? new Date(r.generatedAt).toLocaleString() : 'N/A'}</li>`;
        });
        html += '</ul>';
        document.getElementById('historyResultOutput').innerHTML = html;
    }
}

async function testIdorFetchProfileHistory() {
    const victimId = document.getElementById('secVictimUserId').value || '999';
    await makeApiCall(`${getBaseUrl()}/api/users/${victimId}/profile-history`, 'GET', null, false, 'sec');
}

async function testIdorFetchRecommendationHistory() {
    const victimId = document.getElementById('secVictimUserId').value || '999';
    await makeApiCall(`${getBaseUrl()}/api/recommendations/user/${victimId}`, 'GET', null, false, 'sec');
}
