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
    document.querySelectorAll('.category-nav button').forEach(item => item.classList.remove('active'));
    element.classList.add('active');
    document.getElementById('searchCategory').value = path;
    setWorkspaceContext(path, 'Entries filed in this category and its accessible data.');
    searchData();
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
