// State
let authToken = localStorage.getItem('hiip_token') || '';
let refreshToken = localStorage.getItem('hiip_refresh_token') || '';
let currentUser = localStorage.getItem('hiip_user') || '';
let currentTags = [];
let dataCache = [];
let categoriesCache = [];
let searchTags = [];
let editingDataId = null;
let dataView = 'all';
let selectedCategory = null;

async function authenticatedFetch(url, options = {}, retry = true) {
    const requestOptions = {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            ...(options.headers || {}),
            ...(authToken ? { 'Authorization': `Bearer ${authToken}` } : {})
        }
    };

    const response = await fetch(url, requestOptions);
    if (response.status !== 401 || !retry || !refreshToken) {
        return response;
    }

    const refreshed = await fetch('/api/v1/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken })
    });

    if (!refreshed.ok) {
        handleLogout();
        return response;
    }

    const tokenData = await refreshed.json();
    authToken = tokenData.accessToken;
    refreshToken = tokenData.refreshToken || refreshToken;
    localStorage.setItem('hiip_token', authToken);
    localStorage.setItem('hiip_refresh_token', refreshToken);

    return authenticatedFetch(url, options, false);
}

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

function showAlert(elementId, message, type = '') {
    const element = document.getElementById(elementId);
    if (!element) return;
    element.innerHTML = message ? `<div class="alert ${type ? `alert-${type}` : ''}">${escapeHtml(message)}</div>` : '';
}

// Initialize
document.addEventListener('DOMContentLoaded', function() {
    if (authToken && currentUser) {
        showDashboard();
    }
});

// Login Handler
async function handleLogin(event) {
    event.preventDefault();
    
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const button = document.getElementById('loginButton');
    const alertDiv = document.getElementById('loginAlert');
    
    button.disabled = true;
    button.textContent = 'Logging in...';
    alertDiv.innerHTML = '';
    
    try {
        const response = await fetch('/api/v1/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        if (response.ok) {
            const data = await response.json();
            authToken = data.accessToken;
            refreshToken = data.refreshToken || '';
            currentUser = data.username;
            
            localStorage.setItem('hiip_token', authToken);
            localStorage.setItem('hiip_refresh_token', refreshToken);
            localStorage.setItem('hiip_user', currentUser);
            
            showDashboard();
        } else {
            const error = await response.json().catch(() => ({ error: 'Login failed' }));
            alertDiv.innerHTML = `<div class="alert alert-error">❌ ${error.error || 'Invalid credentials'}</div>`;
        }
    } catch (error) {
        alertDiv.innerHTML = `<div class="alert alert-error">❌ Connection error: ${error.message}</div>`;
    } finally {
        button.disabled = false;
        button.textContent = 'Login';
    }
}

// Logout Handler
function handleLogout() {
    authToken = '';
    refreshToken = '';
    currentUser = '';
    localStorage.removeItem('hiip_token');
    localStorage.removeItem('hiip_refresh_token');
    localStorage.removeItem('hiip_user');
    
    document.getElementById('dashboard').classList.add('hidden');
    document.getElementById('loginPage').classList.remove('hidden');
    document.getElementById('loginForm').reset();
    
    // Clear data
    dataCache = [];
    editingDataId = null;
    document.getElementById('dataListContainer').innerHTML = `
        <div class="empty-state">
            <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V5h14v14z"/>
                <path d="M7 10h2v7H7zm4-3h2v10h-2zm4 6h2v4h-2z"/>
            </svg>
            <p>No data loaded yet</p>
            <p style="font-size: 0.9rem;">Click "Load All Data" to retrieve data</p>
        </div>
    `;
}

// Show Dashboard
function showDashboard() {
    document.getElementById('loginPage').classList.add('hidden');
    document.getElementById('dashboard').classList.remove('hidden');
    document.getElementById('usernameDisplay').textContent = currentUser;
    document.getElementById('avatarInitial').textContent = currentUser.charAt(0).toUpperCase() || '?';
    
    loadCategories();
    loadAllData();
}

function setActiveNav(activeId) {
    document.querySelectorAll('.nav-item').forEach(item => item.classList.remove('active'));
    document.getElementById(activeId)?.classList.add('active');
}

function setWorkspaceContext(title, subtitle) {
    document.getElementById('workspaceTitle').textContent = title;
    document.getElementById('workspaceSubtitle').textContent = subtitle;
}

function showAllData() {
    dataView = 'all';
    setActiveNav('allDataNav');
    setWorkspaceContext('All data', 'Everything you can access, in one stream.');
    renderDataList();
}

function showOwnedData() {
    dataView = 'owned';
    setActiveNav('ownedDataNav');
    setWorkspaceContext('My data', 'Entries created and maintained by you.');
    renderDataList();
}

function showSharedData() {
    dataView = 'shared';
    setActiveNav('sharedDataNav');
    setWorkspaceContext('Shared with me', 'Read and collaborate on entries from your network.');
    renderDataList();
}

function focusComposer() {
    document.getElementById('dataContent').focus();
    document.getElementById('composerPanel').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

// Tag Input Handlers
function handleTagInput(event) {
    console.log('Key pressed:', event.key);
    if (event.key === 'Enter') {
        event.preventDefault();
        const input = document.getElementById('tagInput');
        const value = input.value.trim();
        
        console.log('Tag value:', value);
        console.log('Current tags before:', currentTags);
        
        if (value && !currentTags.includes(value)) {
            currentTags.push(value);
            console.log('Current tags after:', currentTags);
            renderTags();
            input.value = '';
        } else if (!value) {
            console.log('Empty value, not adding');
        } else {
            console.log('Tag already exists');
        }
    }
}

function removeTag(tag) {
    currentTags = currentTags.filter(t => t !== tag);
    renderTags();
}

function renderTags() {
    const container = document.getElementById('tagsContainer');
    const input = document.getElementById('tagInput');
    
    // Clear all tags except input
    const tags = container.querySelectorAll('.tag');
    tags.forEach(tag => tag.remove());
    
    // Add tags before input
    currentTags.forEach(tag => {
        const tagEl = document.createElement('span');
        tagEl.className = 'tag';
        tagEl.innerHTML = `${tag} <span class="remove" onclick="removeTag('${tag}')">×</span>`;
        container.insertBefore(tagEl, input);
    });
}

function focusTagInput() {
    document.getElementById('tagInput').focus();
}

// Load Categories
async function loadCategories() {
    try {
        const response = await authenticatedFetch('/api/v1/categories/my-categories');

        if (response.ok) {
            categoriesCache = await response.json();
            populateCategoryDatalist();
            renderCategoryNavigation();
        } else if (response.status === 401) {
            handleLogout();
        } else {
            console.error('Failed to load categories');
        }
    } catch (error) {
        console.error('Error loading categories:', error);
    }
}

function renderCategoryNavigation() {
    const navigation = document.getElementById('categoryNav');
    if (!navigation) return;

    const categories = [...categoriesCache].sort((a, b) => a.path.localeCompare(b.path));
    if (categories.length === 0) {
        navigation.innerHTML = '<span class="sidebar-muted">No categories yet</span>';
        return;
    }

    navigation.innerHTML = categories.map(category => `
        <button type="button" title="${escapeHtml(category.path)}" onclick="selectCategory('${encodeURIComponent(category.path)}', this)">
            ${category.isGlobal ? '◇' : '·'} ${escapeHtml(category.path)}
        </button>
    `).join('');
}

function selectCategory(encodedPath, element) {
    const path = decodeURIComponent(encodedPath);
    selectedCategory = categoriesCache.find(category => category.path === path) || null;
    document.querySelectorAll('.category-nav button').forEach(item => item.classList.remove('active'));
    element.classList.add('active');
    document.getElementById('searchCategory').value = path;
    setWorkspaceContext(path, 'Entries filed in this category and its accessible data.');
    renderSharingPanel();
    searchData();
}

function renderSharingPanel() {
    const panel = document.getElementById('sharingPanel');
    if (!selectedCategory) {
        panel.classList.add('hidden');
        return;
    }

    panel.classList.remove('hidden');
    document.getElementById('sharingCategoryName').textContent = selectedCategory.path;
    const isOwner = selectedCategory.createdBy === currentUser;
    document.getElementById('sharingDescription').textContent = isOwner
        ? 'Access applies to this category and its descendants.'
        : `Owned by ${selectedCategory.createdBy || 'another user'}. You can view data here according to your access.`;
    document.getElementById('shareCategoryForm').classList.toggle('hidden', !isOwner);
    document.getElementById('sharingLock').textContent = isOwner ? '⌁' : '◌';
    renderShareList(selectedCategory.sharedWith || [], isOwner);
}

function renderShareList(shares, isOwner) {
    const list = document.getElementById('shareList');
    if (!shares.length) {
        list.innerHTML = '<p class="share-empty">No one has access yet.</p>';
        return;
    }

    list.innerHTML = shares.map(share => {
        const permission = share.canWrite ? 'Can edit' : 'Read only';
        const removeButton = isOwner
            ? `<button type="button" class="share-remove" title="Remove access" onclick="unshareCategory('${encodeURIComponent(share.sharedWithUsername)}')">×</button>`
            : '';
        return `<div class="share-row"><span class="share-avatar">${escapeHtml(share.sharedWithUsername.charAt(0).toUpperCase())}</span><span class="share-person"><strong>${escapeHtml(share.sharedWithUsername)}</strong><small>${permission}</small></span>${removeButton}</div>`;
    }).join('');
}

async function handleShareCategory(event) {
    event.preventDefault();
    if (!selectedCategory || selectedCategory.createdBy !== currentUser) return;

    const recipient = document.getElementById('shareRecipient').value.trim();
    const canWrite = document.getElementById('shareCanWrite').checked;
    const alert = document.getElementById('sharingAlert');
    alert.innerHTML = '';

    try {
        const response = await authenticatedFetch(`/api/v1/categories/${selectedCategory.id}/share`, {
            method: 'POST',
            body: JSON.stringify({ username: recipient, canRead: true, canWrite })
        });
        if (response.ok) {
            const share = await response.json();
            selectedCategory.sharedWith = [...(selectedCategory.sharedWith || []).filter(item => item.sharedWithUsername !== share.sharedWithUsername), share];
            renderSharingPanel();
            document.getElementById('shareCategoryForm').reset();
            alert.innerHTML = '<div class="alert alert-success">Access granted.</div>';
        } else if (response.status === 401) {
            handleLogout();
        } else {
            const message = await response.text();
            alert.innerHTML = `<div class="alert alert-error">${escapeHtml(message || 'Could not grant access.')}</div>`;
        }
    } catch (error) {
        alert.innerHTML = `<div class="alert alert-error">${escapeHtml(error.message)}</div>`;
    }
}

async function unshareCategory(encodedUsername) {
    if (!selectedCategory || selectedCategory.createdBy !== currentUser) return;
    const username = decodeURIComponent(encodedUsername);
    if (!confirm(`Remove ${username}'s access to ${selectedCategory.path}?`)) return;

    const alert = document.getElementById('sharingAlert');
    try {
        const response = await authenticatedFetch(`/api/v1/categories/${selectedCategory.id}/share/${encodeURIComponent(username)}`, { method: 'DELETE' });
        if (response.ok) {
            selectedCategory.sharedWith = (selectedCategory.sharedWith || []).filter(share => share.sharedWithUsername !== username);
            renderSharingPanel();
            alert.innerHTML = '<div class="alert alert-success">Access removed.</div>';
        } else if (response.status === 401) {
            handleLogout();
        } else {
            alert.innerHTML = '<div class="alert alert-error">Could not remove access.</div>';
        }
    } catch (error) {
        alert.innerHTML = `<div class="alert alert-error">${escapeHtml(error.message)}</div>`;
    }
}

function populateCategoryDatalist() {
    const datalist = document.getElementById('categorySuggestions');
    const searchDatalist = document.getElementById('searchCategorySuggestions');
    datalist.innerHTML = '';
    searchDatalist.innerHTML = '';
    
    // Sort categories by path for better UX
    const sortedCategories = [...categoriesCache].sort((a, b) => 
        a.path.localeCompare(b.path)
    );
    
    sortedCategories.forEach(category => {
        const option = document.createElement('option');
        option.value = category.path;
        
        // Add description with access info
        let description = category.isGlobal ? ' (Global)' : ' (My category)';
        if (category.sharedWith && category.sharedWith.length > 0) {
            description = ' (Shared)';
        }
        option.textContent = category.path + description;
        
        datalist.appendChild(option);
        
        // Also add to search datalist
        const searchOption = option.cloneNode(true);
        searchDatalist.appendChild(searchOption);
    });
}

// Category creation modal & JSON schema tree editor
const QUICK_SEARCH_LABEL_PATTERN = /^[A-Za-z_][A-Za-z0-9_]*$/;
let schemaTree = [];
let schemaNodeIdSeq = 0;

function openCategoryModal() {
    document.getElementById('categoryForm').reset();
    document.getElementById('categoryAlert').innerHTML = '';
    schemaTree = [];
    renderSchemaTree();
    populateCategoryParentSelect();
    document.getElementById('categoryModal').classList.remove('hidden');
    document.getElementById('categoryNameInput').focus();
}

function closeCategoryModal() {
    document.getElementById('categoryModal').classList.add('hidden');
}

function populateCategoryParentSelect() {
    const select = document.getElementById('categoryParentInput');
    const sorted = [...categoriesCache].sort((a, b) => a.path.localeCompare(b.path));
    select.innerHTML = '<option value="">None (root category)</option>' +
        sorted.map(category => `<option value="${category.id}">${escapeHtml(category.path)}</option>`).join('');
}

function findSchemaNode(nodes, id) {
    for (const node of nodes) {
        if (node.id === id) return node;
        const found = findSchemaNode(node.children, id);
        if (found) return found;
    }
    return null;
}

function removeSchemaNodeFromList(nodes, id) {
    const index = nodes.findIndex(node => node.id === id);
    if (index !== -1) {
        nodes.splice(index, 1);
        return true;
    }
    return nodes.some(node => removeSchemaNodeFromList(node.children, id));
}

function addSchemaNode(parentId) {
    const node = {
        id: ++schemaNodeIdSeq,
        name: '',
        type: 'string',
        itemType: 'string',
        required: false,
        quickSearch: false,
        quickSearchLabel: '',
        children: []
    };

    if (parentId === null) {
        schemaTree.push(node);
    } else {
        const parent = findSchemaNode(schemaTree, parentId);
        if (!parent) return;
        parent.children.push(node);
    }
    renderSchemaTree();
}

function removeSchemaNode(id) {
    removeSchemaNodeFromList(schemaTree, id);
    renderSchemaTree();
}

function updateSchemaNode(id, field, value, rerender = true) {
    const node = findSchemaNode(schemaTree, id);
    if (!node) return;
    node[field] = value;
    if (rerender) renderSchemaTree();
}

function schemaNodeAcceptsChildren(node) {
    return node.type === 'object' || (node.type === 'array' && node.itemType === 'object');
}

function renderSchemaTree() {
    const container = document.getElementById('schemaTree');
    if (!container) return;
    container.innerHTML = renderSchemaNodes(schemaTree) || '<p class="schema-empty">No fields defined. Entries can contain any JSON content.</p>';
}

function renderSchemaNodes(nodes) {
    return nodes.map(node => renderSchemaNode(node)).join('');
}

function renderSchemaNode(node) {
    const typeOptions = ['string', 'number', 'integer', 'boolean', 'object', 'array', 'null'];
    const typeSelect = `<select onchange="updateSchemaNode(${node.id}, 'type', this.value)">
        ${typeOptions.map(type => `<option value="${type}" ${node.type === type ? 'selected' : ''}>${type}</option>`).join('')}
    </select>`;

    const itemTypeOptions = ['string', 'number', 'integer', 'boolean', 'object'];
    const itemTypeSelect = node.type === 'array'
        ? `<select title="Array item type" onchange="updateSchemaNode(${node.id}, 'itemType', this.value)">
            ${itemTypeOptions.map(type => `<option value="${type}" ${node.itemType === type ? 'selected' : ''}>${type}[]</option>`).join('')}
        </select>`
        : '';

    const quickSearchLabelInput = node.quickSearch
        ? `<input type="text" class="schema-node-label-input" placeholder="Label (default: ${escapeHtml(node.name) || 'field name'})" value="${escapeHtml(node.quickSearchLabel)}" oninput="updateSchemaNode(${node.id}, 'quickSearchLabel', this.value, false)">`
        : '';

    const addChildButton = schemaNodeAcceptsChildren(node)
        ? `<button type="button" class="schema-node-add" onclick="addSchemaNode(${node.id})">＋ field</button>`
        : '';

    const childrenHtml = schemaNodeAcceptsChildren(node) && node.children.length > 0
        ? `<div class="schema-node-children">${renderSchemaNodes(node.children)}</div>`
        : '';

    return `
        <div class="schema-node">
            <div class="schema-node-row">
                <input type="text" placeholder="field name" value="${escapeHtml(node.name)}" oninput="updateSchemaNode(${node.id}, 'name', this.value, false)">
                ${typeSelect}
                ${itemTypeSelect}
                <label class="schema-node-flag"><input type="checkbox" ${node.required ? 'checked' : ''} onchange="updateSchemaNode(${node.id}, 'required', this.checked, false)"> Required</label>
                <label class="schema-node-flag"><input type="checkbox" ${node.quickSearch ? 'checked' : ''} onchange="updateSchemaNode(${node.id}, 'quickSearch', this.checked)"> Quick search</label>
                ${quickSearchLabelInput}
                ${addChildButton}
                <button type="button" class="schema-node-remove" title="Remove field" onclick="removeSchemaNode(${node.id})">×</button>
            </div>
            ${childrenHtml}
        </div>
    `;
}

/**
 * Recursively converts the schema tree into a JSON Schema fragment plus the collected
 * x-quick-search entries (JSON Path + label), path segments account for array item traversal.
 */
function buildSchemaFromNodes(nodes, pathPrefix) {
    const properties = {};
    const required = [];
    const quickSearch = [];

    nodes.forEach(node => {
        const name = node.name.trim();
        if (!name) return;

        const currentPath = pathPrefix + name;
        let fieldSchema;

        if (node.type === 'object') {
            const sub = buildSchemaFromNodes(node.children, `${currentPath}.`);
            fieldSchema = { type: 'object', properties: sub.properties };
            if (sub.required.length) fieldSchema.required = sub.required;
            quickSearch.push(...sub.quickSearch);
        } else if (node.type === 'array') {
            if (node.itemType === 'object') {
                const sub = buildSchemaFromNodes(node.children, `${currentPath}[*].`);
                const itemSchema = { type: 'object', properties: sub.properties };
                if (sub.required.length) itemSchema.required = sub.required;
                fieldSchema = { type: 'array', items: itemSchema };
                quickSearch.push(...sub.quickSearch);
            } else {
                fieldSchema = { type: 'array', items: { type: node.itemType } };
            }
        } else {
            fieldSchema = { type: node.type };
        }

        properties[name] = fieldSchema;
        if (node.required) required.push(name);
        if (node.quickSearch) {
            const label = node.quickSearchLabel.trim() || name;
            quickSearch.push({ path: `$.${currentPath}`, label });
        }
    });

    return { properties, required, quickSearch };
}

function buildJsonSchema() {
    const { properties, required, quickSearch } = buildSchemaFromNodes(schemaTree, '');
    if (Object.keys(properties).length === 0) return null;

    const schema = {
        '$schema': 'https://json-schema.org/draft/2020-12/schema',
        type: 'object',
        properties
    };
    if (required.length) schema.required = required;
    if (quickSearch.length) {
        for (const entry of quickSearch) {
            if (!QUICK_SEARCH_LABEL_PATTERN.test(entry.label)) {
                throw new Error(`Quick search label "${entry.label}" must be a single word (letters, digits, underscore, starting with a letter or underscore).`);
            }
        }
        schema['x-quick-search'] = quickSearch;
    }
    return schema;
}

async function handleCreateCategory(event) {
    event.preventDefault();

    const name = document.getElementById('categoryNameInput').value.trim();
    const path = document.getElementById('categoryPathInput').value.trim();
    const parentId = document.getElementById('categoryParentInput').value;
    const isGlobal = document.getElementById('categoryGlobalInput').checked;
    const alertDiv = document.getElementById('categoryAlert');

    let schema;
    try {
        schema = buildJsonSchema();
    } catch (error) {
        alertDiv.innerHTML = `<div class="alert alert-error">❌ ${escapeHtml(error.message)}</div>`;
        return;
    }

    const requestData = {
        name,
        path: path || null,
        parentId: parentId ? Number(parentId) : null,
        isGlobal,
        schema
    };

    alertDiv.innerHTML = '<div class="alert">Creating category...</div>';

    try {
        const response = await authenticatedFetch('/api/v1/categories', {
            method: 'POST',
            body: JSON.stringify(requestData)
        });

        if (response.ok) {
            alertDiv.innerHTML = '<div class="alert alert-success">Category created successfully.</div>';
            await loadCategories();
            setTimeout(closeCategoryModal, 900);
        } else if (response.status === 401) {
            handleLogout();
        } else {
            const error = await response.text();
            alertDiv.innerHTML = `<div class="alert alert-error">❌ ${escapeHtml(error)}</div>`;
        }
    } catch (error) {
        alertDiv.innerHTML = `<div class="alert alert-error">❌ Error: ${escapeHtml(error.message)}</div>`;
    }
}

// Create Data Handler
async function handleCreateData(event) {
    event.preventDefault();
    
    const content = document.getElementById('dataContent').value.trim();
    const category = document.getElementById('categoryInput').value.trim();
    const alertDiv = document.getElementById('createAlert');
    
    if (!content) {
        alertDiv.innerHTML = '<div class="alert alert-error">❌ Content is required</div>';
        return;
    }

    // Validate JSON
    let jsonContent;
    try {
        jsonContent = JSON.parse(content);
    } catch (e) {
        alertDiv.innerHTML = '<div class="alert alert-error">❌ Invalid JSON format</div>';
        return;
    }
    
    alertDiv.innerHTML = `<div class="alert">${editingDataId ? 'Saving changes...' : 'Creating data...'}</div>`;
    
    // Debug logging
    const requestData = {
        content: jsonContent,
        tags: currentTags,
        category: category || null
    };
    console.log('Sending data:', requestData);
    console.log('Tags array:', currentTags);
    console.log('Category:', category);
    
    try {
        const response = await authenticatedFetch(editingDataId ? `/api/v1/data/${editingDataId}` : '/api/v1/data', {
            method: editingDataId ? 'PUT' : 'POST',
            body: JSON.stringify(requestData)
        });

        if (response.ok) {
            const data = await response.json();
            alertDiv.innerHTML = `<div class="alert alert-success">${editingDataId ? 'Changes saved successfully.' : 'Data created successfully.'}</div>`;
            clearCreateForm();
            
            // Refresh data list and categories
            setTimeout(() => {
                refreshData();
                loadCategories();
                alertDiv.innerHTML = '';
            }, 2000);
        } else if (response.status === 401) {
            handleLogout();
        } else {
            const error = await response.text();
            alertDiv.innerHTML = `<div class="alert alert-error">❌ Failed to create data: ${error}</div>`;
        }
    } catch (error) {
        alertDiv.innerHTML = `<div class="alert alert-error">❌ Error: ${error.message}</div>`;
    }
}

function clearCreateForm() {
    document.getElementById('dataContent').value = '';
    document.getElementById('categoryInput').value = '';
    currentTags = [];
    editingDataId = null;
    document.getElementById('createHeading').textContent = '📝 Create Data';
    document.getElementById('saveDataButton').textContent = 'Create Data';
    document.getElementById('cancelEditButton').classList.add('hidden');
    renderTags();
}

// Load All Data
async function loadAllData() {
    const container = document.getElementById('dataListContainer');
    const alertDiv = document.getElementById('queryAlert');
    
    container.innerHTML = '<div class="loading">Loading data</div>';
    alertDiv.innerHTML = '';
    
    try {
        const response = await authenticatedFetch('/api/v1/data/accessible');

        if (response.ok) {
            dataCache = await response.json();
            renderDataList();
        } else if (response.status === 401) {
            handleLogout();
        } else {
            container.innerHTML = '<div class="empty-state"><p>Failed to load data</p></div>';
            alertDiv.innerHTML = '<div class="alert alert-error">❌ Failed to load data</div>';
        }
    } catch (error) {
        container.innerHTML = '<div class="empty-state"><p>Connection error</p></div>';
        alertDiv.innerHTML = `<div class="alert alert-error">❌ Error: ${error.message}</div>`;
    }
}

function refreshData() {
    loadAllData();
}

// Render Data List
function renderDataList() {
    const container = document.getElementById('dataListContainer');
    const visibleData = dataCache.filter(item => {
        if (dataView === 'owned') return item.owner === currentUser;
        if (dataView === 'shared') return item.owner !== currentUser;
        return true;
    });
    document.getElementById('dataCount').textContent = dataCache.length;
    document.getElementById('resultCount').textContent = `${visibleData.length} ${visibleData.length === 1 ? 'entry' : 'entries'}`;
    
    if (visibleData.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <svg viewBox="0 0 24 24" fill="currentColor">
                    <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V5h14v14z"/>
                    <path d="M7 10h2v7H7zm4-3h2v10h-2zm4 6h2v4h-2z"/>
                </svg>
                <p>No data found</p>
                <p style="font-size: 0.9rem;">Create some data to see it here</p>
            </div>
        `;
        return;
    }
    
    let html = '';
    visibleData.forEach(item => {
        const tagsHtml = item.tags && item.tags.length > 0
            ? item.tags.map(tag => `<span class="tag">${escapeHtml(tag)}</span>`).join('')
            : '<span style="color: #999;">No tags</span>';
        
        const categoryHtml = item.category 
            ? `<div class="data-item-category">📁 ${escapeHtml(item.category)}</div>`
            : '';
        
        const contentStr = JSON.stringify(item.content, null, 2);
        const isOwner = item.owner === currentUser;
        const actionsHtml = isOwner
            ? `<button class="button secondary" onclick="editData(${item.id})">Edit</button>
               <button class="button danger" onclick="deleteData(${item.id})">Delete</button>`
            : '<span class="read-only-badge">Read only</span>';
        
        html += `
            <div class="data-item">
                <div class="data-item-header">
                    <span class="data-item-id">#${item.id}</span>
                    <div class="data-item-actions">
                        <button class="button secondary" onclick="viewData(${item.id})">View</button>
                        ${actionsHtml}
                    </div>
                </div>
                ${categoryHtml}
                <div class="data-item-content">${escapeHtml(contentStr)}</div>
                <div class="data-item-tags">${tagsHtml}</div>
                <div class="data-item-meta">
                    Owner: ${escapeHtml(item.owner)} | Created: ${new Date(item.createdAt).toLocaleString()}
                </div>
            </div>
        `;
    });
    
    container.innerHTML = html;
}

function editData(id) {
    const item = dataCache.find(data => data.id === id);
    if (!item || item.owner !== currentUser) return;

    editingDataId = id;
    document.getElementById('dataContent').value = JSON.stringify(item.content, null, 2);
    document.getElementById('categoryInput').value = item.category || '';
    currentTags = item.tags ? [...item.tags] : [];
    document.getElementById('createHeading').textContent = `✏️ Edit Data #${id}`;
    document.getElementById('saveDataButton').textContent = 'Save Changes';
    document.getElementById('cancelEditButton').classList.remove('hidden');
    renderTags();
    document.getElementById('dataContent').focus();
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// View Data
function viewData(id) {
    const item = dataCache.find(d => d.id === id);
    if (item) {
        alert('Data Details:\n\n' + JSON.stringify(item, null, 2));
    }
}

// Delete Data
async function deleteData(id) {
    if (!confirm(`Are you sure you want to delete data #${id}?`)) {
        return;
    }
    
    const alertDiv = document.getElementById('queryAlert');
    
    try {
        const response = await authenticatedFetch(`/api/v1/data/${id}`, {
            method: 'DELETE',
        });

        if (response.ok) {
            alertDiv.innerHTML = '<div class="alert alert-success">✅ Data deleted successfully</div>';
            refreshData();
            setTimeout(() => alertDiv.innerHTML = '', 3000);
        } else if (response.status === 401) {
            handleLogout();
        } else {
            alertDiv.innerHTML = '<div class="alert alert-error">❌ Failed to delete data</div>';
        }
    } catch (error) {
        alertDiv.innerHTML = `<div class="alert alert-error">❌ Error: ${error.message}</div>`;
    }
}

// Search Tag Input Handlers
function handleSearchTagInput(event) {
    if (event.key === 'Enter') {
        event.preventDefault();
        const input = document.getElementById('searchTagsInput');
        const value = input.value.trim();
        
        if (value && !searchTags.includes(value)) {
            searchTags.push(value);
            renderSearchTags();
            input.value = '';
        }
    }
}

function removeSearchTag(tag) {
    searchTags = searchTags.filter(t => t !== tag);
    renderSearchTags();
}

function renderSearchTags() {
    const container = document.getElementById('searchTagsContainer');
    const input = document.getElementById('searchTagsInput');
    
    // Clear all tags except input
    const tags = container.querySelectorAll('.tag');
    tags.forEach(tag => tag.remove());
    
    // Add tags before input
    searchTags.forEach(tag => {
        const tagEl = document.createElement('span');
        tagEl.className = 'tag';
        tagEl.innerHTML = `${tag} <span class="remove" onclick="removeSearchTag('${tag}')">×</span>`;
        container.insertBefore(tagEl, input);
    });
}

function focusSearchTagInput() {
    document.getElementById('searchTagsInput').focus();
}

// Search Data
async function searchData() {
    const category = document.getElementById('searchCategory').value.trim();
    const container = document.getElementById('dataListContainer');
    const alertDiv = document.getElementById('queryAlert');
    
    // Build query parameters
    const params = new URLSearchParams();
    
    if (searchTags.length > 0) {
        searchTags.forEach(tag => params.append('tags', tag));
    }
    
    if (category) {
        params.append('category', category);
    }
    
    const queryString = params.toString();
    const url = queryString ? `/api/v1/data/search?${queryString}` : '/api/v1/data';
    
    container.innerHTML = '<div class="loading">Searching...</div>';
    alertDiv.innerHTML = '';
    
    try {
        const response = await authenticatedFetch(`/api/v1/data/accessible/search?${params.toString()}`);

        if (response.ok) {
            dataCache = await response.json();
            renderDataList();
            
            // Show search summary
            let summary = 'Showing ';
            if (searchTags.length > 0 || category) {
                summary += 'filtered results';
                if (searchTags.length > 0) {
                    summary += ` (tags: ${searchTags.join(', ')})`;
                }
                if (category) {
                    summary += ` (category: ${category})`;
                }
            } else {
                summary += 'all data';
            }
            summary += ` - ${dataCache.length} item${dataCache.length !== 1 ? 's' : ''} found`;
            document.getElementById('resultSummary').textContent = summary;
            alertDiv.innerHTML = `<div class="alert">${summary}</div>`;
            setTimeout(() => alertDiv.innerHTML = '', 5000);
        } else if (response.status === 401) {
            handleLogout();
        } else {
            container.innerHTML = '<div class="empty-state"><p>Search failed</p></div>';
            alertDiv.innerHTML = '<div class="alert alert-error">❌ Failed to search data</div>';
        }
    } catch (error) {
        container.innerHTML = '<div class="empty-state"><p>Connection error</p></div>';
        alertDiv.innerHTML = `<div class="alert alert-error">❌ Error: ${error.message}</div>`;
    }
}

function clearSearchFilters() {
    document.getElementById('searchCategory').value = '';
    searchTags = [];
    renderSearchTags();
    document.getElementById('queryAlert').innerHTML = '';
}
