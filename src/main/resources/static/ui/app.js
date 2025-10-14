// State
let authToken = localStorage.getItem('hiip_token') || '';
let currentUser = localStorage.getItem('hiip_user') || '';
let currentTags = [];
let dataCache = [];

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
            currentUser = data.username;
            
            localStorage.setItem('hiip_token', authToken);
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
    currentUser = '';
    localStorage.removeItem('hiip_token');
    localStorage.removeItem('hiip_user');
    
    document.getElementById('dashboard').classList.add('hidden');
    document.getElementById('loginPage').classList.remove('hidden');
    document.getElementById('loginForm').reset();
    
    // Clear data
    dataCache = [];
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

// Create Data Handler
async function handleCreateData(event) {
    event.preventDefault();
    
    const content = document.getElementById('dataContent').value.trim();
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
    
    alertDiv.innerHTML = '<div class="alert">Creating data...</div>';
    
    // Debug logging
    const requestData = {
        content: jsonContent,
        tags: currentTags
    };
    console.log('Sending data:', requestData);
    console.log('Tags array:', currentTags);
    
    try {
        const response = await fetch('/api/v1/data', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${authToken}`
            },
            body: JSON.stringify(requestData)
        });

        if (response.ok) {
            const data = await response.json();
            alertDiv.innerHTML = '<div class="alert alert-success">✅ Data created successfully!</div>';
            clearCreateForm();
            
            // Refresh data list
            setTimeout(() => {
                refreshData();
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
    currentTags = [];
    renderTags();
}

// Load All Data
async function loadAllData() {
    const container = document.getElementById('dataListContainer');
    const alertDiv = document.getElementById('queryAlert');
    
    container.innerHTML = '<div class="loading">Loading data</div>';
    alertDiv.innerHTML = '';
    
    try {
        const response = await fetch('/api/v1/data', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${authToken}`,
                'Content-Type': 'application/json'
            }
        });

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
    
    if (dataCache.length === 0) {
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
    dataCache.forEach(item => {
        const tagsHtml = item.tags && item.tags.length > 0
            ? item.tags.map(tag => `<span class="tag">${tag}</span>`).join('')
            : '<span style="color: #999;">No tags</span>';
        
        const contentStr = JSON.stringify(item.content, null, 2);
        
        html += `
            <div class="data-item">
                <div class="data-item-header">
                    <span class="data-item-id">#${item.id}</span>
                    <div class="data-item-actions">
                        <button class="button secondary" onclick="viewData(${item.id})">View</button>
                        <button class="button danger" onclick="deleteData(${item.id})">Delete</button>
                    </div>
                </div>
                <div class="data-item-content">${contentStr}</div>
                <div class="data-item-tags">${tagsHtml}</div>
                <div class="data-item-meta">
                    Owner: ${item.owner} | Created: ${new Date(item.createdAt).toLocaleString()}
                </div>
            </div>
        `;
    });
    
    container.innerHTML = html;
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
        const response = await fetch(`/api/v1/data/${id}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
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
