# UI Enhancement: Advanced Search with Category and Tags Filters

## Overview
Added a comprehensive search interface to the Query Data panel, allowing users to filter data by category and/or tags directly from the UI. Both filters are optional and can be used independently or in combination.

## Features

### 🔍 Search Filters Panel
A new collapsible search section with:
1. **Category Filter** - Auto-complete input with existing categories
2. **Tags Filter** - Interactive tag input similar to the create form
3. **Search Button** - Execute search with selected filters
4. **Clear Filters** - Reset all search criteria
5. **Load All** - Quick access to unfiltered data

### Visual Design
- Light gray background (`#f5f5f5`) to distinguish from data list
- Rounded corners for modern appearance
- Consistent styling with the create form
- Clear visual hierarchy

## User Interface

### Search Filters Section
```
┌─────────────────────────────────────────┐
│ 🔍 Query Data                           │
├─────────────────────────────────────────┤
│ Search Filters                          │
│                                         │
│ Category: [________________] ▼          │
│           (dropdown with categories)    │
│                                         │
│ Tags: [tag1 ×] [tag2 ×] [_______]     │
│       (click × to remove)               │
│                                         │
│ [🔍 Search] [Clear Filters] [Load All] │
└─────────────────────────────────────────┘
```

## Implementation Details

### HTML Changes (`index.html`)

Added search filters section in the Query Data panel:

```html
<div class="search-filters">
    <h3>Search Filters</h3>
    
    <!-- Category filter with auto-complete -->
    <div class="form-group">
        <label for="searchCategory">Category</label>
        <input type="text" id="searchCategory" list="searchCategorySuggestions">
        <datalist id="searchCategorySuggestions"></datalist>
    </div>
    
    <!-- Tags filter with interactive tag input -->
    <div class="form-group">
        <label for="searchTagsInput">Tags</label>
        <div class="tags-input" id="searchTagsContainer">
            <input type="text" id="searchTagsInput">
        </div>
    </div>
    
    <!-- Action buttons -->
    <button onclick="searchData()">🔍 Search</button>
    <button onclick="clearSearchFilters()">Clear Filters</button>
    <button onclick="loadAllData()">Load All</button>
</div>
```

### JavaScript Changes (`app.js`)

#### 1. State Management
```javascript
let searchTags = [];  // New state for search tags
```

#### 2. Category Datalist Population
Updated `populateCategoryDatalist()` to populate both:
- Create form category datalist
- Search form category datalist

```javascript
function populateCategoryDatalist() {
    const datalist = document.getElementById('categorySuggestions');
    const searchDatalist = document.getElementById('searchCategorySuggestions');
    
    // Populate both datalists with categories
    sortedCategories.forEach(category => {
        datalist.appendChild(option);
        searchDatalist.appendChild(option.cloneNode(true));
    });
}
```

#### 3. Search Tag Handlers
Similar to create form tags, but for search:

```javascript
function handleSearchTagInput(event) {
    // Add tag on Enter key
}

function removeSearchTag(tag) {
    // Remove tag from search criteria
}

function renderSearchTags() {
    // Display search tags in UI
}

function focusSearchTagInput() {
    // Focus helper for container click
}
```

#### 4. Search Function
Main search logic with API call:

```javascript
async function searchData() {
    const category = document.getElementById('searchCategory').value.trim();
    
    // Build query parameters
    const params = new URLSearchParams();
    if (searchTags.length > 0) {
        searchTags.forEach(tag => params.append('tags', tag));
    }
    if (category) {
        params.append('category', category);
    }
    
    // Call search API: /api/v1/data/search?tags=tag1&tags=tag2&category=path
    const url = queryString ? `/api/v1/data/search?${queryString}` : '/api/v1/data';
    
    // Fetch and display results
    // Show search summary
}
```

#### 5. Clear Filters Function
```javascript
function clearSearchFilters() {
    document.getElementById('searchCategory').value = '';
    searchTags = [];
    renderSearchTags();
}
```

## User Workflow

### Basic Usage

1. **Search by Category Only**
   - Click on Category field
   - Select or type category path
   - Click "Search" button
   - Results filtered by category

2. **Search by Tags Only**
   - Click in Tags field
   - Type tag name and press Enter
   - Repeat for multiple tags
   - Click "Search" button
   - Results matching any of the tags

3. **Combined Search**
   - Select/type category
   - Add one or more tags
   - Click "Search" button
   - Results matching tags AND in the category

4. **Load All Data**
   - Click "Load All" button
   - Clears any active filters
   - Shows all user's data

5. **Clear Filters**
   - Click "Clear Filters" button
   - Removes all search criteria
   - Doesn't execute search (data remains displayed)

### Search Results Display

After search, a summary message appears:
```
ℹ️ Showing filtered results (tags: urgent, important) (category: work/projects) - 5 items found
```

Or for "Load All":
```
ℹ️ Showing all data - 23 items found
```

## Search Behavior

| Category | Tags | Result |
|----------|------|--------|
| Empty | Empty | All data (same as Load All) |
| Set | Empty | Data in that category |
| Empty | Set | Data with any of those tags |
| Set | Set | Data with tags AND in category |

## Technical Details

### API Integration

Uses the enhanced search endpoint:
```
GET /api/v1/data/search?tags=tag1&tags=tag2&category=work/projects
```

Parameters:
- `tags` (optional, repeatable): Tag names to search for
- `category` (optional): Category path to filter by

### Query String Building

```javascript
const params = new URLSearchParams();
searchTags.forEach(tag => params.append('tags', tag));  // Multiple tags
if (category) params.append('category', category);      // Single category

// Results in: ?tags=urgent&tags=important&category=work/projects
```

### Category Auto-Complete

- Populated from `/api/v1/categories/my-categories` API
- Shows categories user can access (owned, global, shared)
- Visual indicators: (Global), (My category), (Shared)
- Same behavior as create form category field

### Tag Management

- Interactive tag input with visual chips
- Click × to remove tags
- Press Enter to add tags
- Same UX as create form tags

## Styling

Consistent with existing UI:
- Same tag chip styling
- Same button styling
- Same form input styling
- Search filters panel has distinct background

## Benefits

1. **Easy Filtering** - Find specific data without manual scrolling
2. **Flexible Search** - Use category, tags, or both
3. **Visual Feedback** - Clear indication of active filters
4. **Reusable Categories** - Auto-complete from existing categories
5. **Batch Operations** - Multiple tags in single search
6. **Quick Reset** - Clear filters or load all with one click

## Usage Examples

### Example 1: Find All Work Projects
```
Category: work/projects
Tags: (empty)
→ Shows all data in work/projects category
```

### Example 2: Find Urgent Items
```
Category: (empty)
Tags: urgent, important
→ Shows all data tagged with "urgent" OR "important"
```

### Example 3: Find Urgent Work Projects
```
Category: work/projects
Tags: urgent
→ Shows data tagged "urgent" AND in work/projects category
```

### Example 4: Browse All Data
```
Category: (empty)
Tags: (empty)
Click "Load All"
→ Shows all user's data
```

## Testing Scenarios

- [x] Search by category only
- [x] Search by tags only
- [x] Search by category and tags combined
- [x] Search with no filters (returns all)
- [x] Clear filters functionality
- [x] Load All button
- [x] Category auto-complete works
- [x] Tag add/remove works
- [x] Search summary displays correctly
- [x] Non-existent category returns empty results
- [x] Multiple tags can be added
- [x] Results update on each search

## Related Files

- `/src/main/resources/static/ui/index.html` - HTML structure with search filters
- `/src/main/resources/static/ui/app.js` - JavaScript search logic
- `/src/main/resources/static/ui/styles.css` - Styling (existing styles reused)

## Future Enhancements

Potential improvements:
- Save search criteria in localStorage
- Search history dropdown
- Advanced filters (date range, owner)
- Export search results
- Saved searches/bookmarks
- Real-time search as you type
- Search result sorting options
- Highlight search terms in results

## Notes

- Search is performed server-side (not client-side filtering)
- Results are cached in `dataCache` for consistency
- Category names are case-sensitive
- Tags use OR logic (any tag matches)
- Combined search uses AND logic between category and tags
- Empty search = load all data
