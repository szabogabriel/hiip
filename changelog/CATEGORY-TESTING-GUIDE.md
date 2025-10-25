# Quick Start Guide - Category Feature Testing

## Application is Running! ✅

The application is successfully running on **http://localhost:8080**

## Quick Test Steps

### 1. Access the UI
Open your browser and navigate to:
```
http://localhost:8080/ui
```

### 2. Login
Use your existing credentials to login

### 3. Create Data with Category

In the "Create Data" panel:

**Content (JSON):**
```json
{
  "product": "Gaming Laptop",
  "brand": "ASUS",
  "price": 1299,
  "specs": {
    "cpu": "Intel i7",
    "ram": "16GB",
    "storage": "512GB SSD"
  }
}
```

**Category:**
```
electronics/computers/laptops
```

**Tags:**
- Add tags like: `gaming`, `sale`, `featured`

Click **"Create Data"**

### 4. Observe the Result

You should see:
- ✅ Data created successfully message
- The new data item appears in the query panel
- Category badge displayed: 📁 electronics/computers/laptops

### 5. Test Category API via Swagger

Navigate to Swagger UI:
```
http://localhost:8080/swagger-ui.html
```

**Try these endpoints:**

1. **GET /api/v1/categories**
   - Click "Try it out" → "Execute"
   - Should show all categories in flat list

2. **GET /api/v1/categories/tree**
   - Click "Try it out" → "Execute"
   - Should show hierarchical tree structure

3. **GET /api/v1/categories/search**
   - Enter prefix: `electronics`
   - Should return all categories starting with "electronics"

### 6. Create More Test Data

Try creating data with different category paths:

**Books:**
```json
{"title": "Dune", "author": "Frank Herbert", "year": 1965}
```
Category: `books/fiction/scifi`

**Clothing:**
```json
{"item": "T-Shirt", "size": "M", "color": "blue"}
```
Category: `products/clothing/mens/shirts`

**Documents:**
```json
{"type": "Report", "quarter": "Q1", "year": 2025}
```
Category: `documents/reports/2025/q1`

### 7. Verify Category Hierarchy

After creating these items, check the category tree:

**GET /api/v1/categories/tree** should show:
```json
[
  {
    "name": "electronics",
    "path": "electronics",
    "children": [
      {
        "name": "computers",
        "path": "electronics/computers",
        "children": [
          {
            "name": "laptops",
            "path": "electronics/computers/laptops",
            "children": []
          }
        ]
      }
    ]
  },
  {
    "name": "books",
    "path": "books",
    "children": [
      {
        "name": "fiction",
        "path": "books/fiction",
        "children": [
          {
            "name": "scifi",
            "path": "books/fiction/scifi",
            "children": []
          }
        ]
      }
    ]
  },
  {
    "name": "products",
    "path": "products",
    "children": [...]
  },
  {
    "name": "documents",
    "path": "documents",
    "children": [...]
  }
]
```

## Test Category Creation Scenarios

### Scenario 1: New Category Path
Create data with category: `toys/outdoor/sports/soccer`
- ✅ Should create 4 categories automatically
- ✅ Should show complete hierarchy

### Scenario 2: Existing Parent Category
Create data with category: `electronics/phones`
- ✅ Should reuse existing "electronics" category
- ✅ Should create only "phones" subcategory

### Scenario 3: Path Normalization
Try these and verify they all create the same category:
- `  electronics / computers  `
- `//electronics//computers//`
- `electronics/computers`

All should result in: `electronics/computers`

### Scenario 4: Data Without Category
Create data without entering a category:
```json
{"message": "Hello World"}
```
- ✅ Should work fine (category is optional)
- ✅ No category badge shown

## Verify UI Features

### Category Display
- [ ] Category badge shows folder icon 📁
- [ ] Category has blue background color
- [ ] Complete path is displayed

### Create Form
- [ ] Category input field is present
- [ ] Placeholder text shows example
- [ ] Help text explains slash separator
- [ ] Form clears category field after submission

### Data List
- [ ] Data with categories shows badge
- [ ] Data without categories doesn't show badge
- [ ] All other fields (tags, content) still display correctly

## API Testing with curl

If you prefer command-line testing:

```bash
# Get your JWT token first (save from login response)
TOKEN="your_jwt_token_here"

# Create data with category
curl -X POST http://localhost:8080/api/v1/data \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": {"test": "value"},
    "tags": ["test"],
    "category": "test/category/path"
  }'

# Get all categories
curl -X GET http://localhost:8080/api/v1/categories \
  -H "Authorization: Bearer $TOKEN"

# Get category tree
curl -X GET http://localhost:8080/api/v1/categories/tree \
  -H "Authorization: Bearer $TOKEN"

# Search categories
curl -X GET "http://localhost:8080/api/v1/categories/search?prefix=test" \
  -H "Authorization: Bearer $TOKEN"
```

## Expected Database Schema

After creating some categories, check H2 console:
```
http://localhost:8080/h2-console
```

**Tables created:**
- `categories` - stores category tree
- `data_storage` - has `category_id` foreign key column

**Sample query:**
```sql
SELECT * FROM categories ORDER BY path;
```

Should show the hierarchical structure with parent-child relationships.

## Troubleshooting

### Category not showing in UI
- Check browser console for JavaScript errors
- Verify the response from `/api/v1/data` includes category field
- Refresh the page

### Category not created
- Check application logs for errors
- Verify JSON format is valid
- Check if category path is properly normalized

### Tree structure not showing
- Make sure to use `/api/v1/categories/tree` endpoint
- Check that parent-child relationships are correct in database

## Success Indicators

✅ Can create data with category paths  
✅ Categories are created automatically  
✅ Category tree can be retrieved  
✅ Category badge shows in UI  
✅ Path normalization works  
✅ Existing categories are reused  
✅ No errors in application logs  

## Next Steps

Once testing is complete:
1. Commit the changes to git
2. Deploy to your environment
3. Consider implementing additional features from CATEGORY-STRUCTURE.md
4. Add integration tests for category functionality

## Support

For detailed documentation, see:
- `CATEGORY-STRUCTURE.md` - Complete feature documentation
- `CATEGORY-IMPLEMENTATION-SUMMARY.md` - Implementation details
- Swagger UI: http://localhost:8080/swagger-ui.html
