package com.hiip.datastorage.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Category entity representing a hierarchical category structure.
 * Categories form a tree structure where each category can have a parent category.
 * The complete path from root to this category is stored in the 'path' field.
 */
@Entity
@Table(name = "categories", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"path"})
})
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Complete path from root to this category, separated by slashes.
     * Example: "electronics/computers/laptops"
     */
    @Column(nullable = false, unique = true, length = 500)
    private String path;

    /**
     * Parent category reference (null for root categories)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    /**
     * Child categories
     */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Category> children = new ArrayList<>();

    /**
     * Username of the user who created this category
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    /**
     * Whether this is a global category (accessible to all users)
     * Global categories can only be created by admins
     */
    @Column(nullable = false)
    private boolean isGlobal = false;

    /**
     * Users this category is shared with and their permissions
     */
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CategoryShare> sharedWith = new ArrayList<>();

    /**
     * Optional JSON schema used to validate entries created under this category.
     */
    @Column(name = "json_schema", columnDefinition = "TEXT")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode jsonSchema;

    /**
     * Number of "quick search" path/value columns available on categories and data entries.
     */
    public static final int QUICK_SEARCH_COLUMN_COUNT = 10;

    /**
     * JSON Path expressions (resolved against a category's schema) used to populate the
     * corresponding quick-search columns on {@link DataStorage} entries for indexed lookups.
     */
    @Column(name = "quick_search_path_1", length = 500)
    private String quickSearchPath1;

    @Column(name = "quick_search_path_2", length = 500)
    private String quickSearchPath2;

    @Column(name = "quick_search_path_3", length = 500)
    private String quickSearchPath3;

    @Column(name = "quick_search_path_4", length = 500)
    private String quickSearchPath4;

    @Column(name = "quick_search_path_5", length = 500)
    private String quickSearchPath5;

    @Column(name = "quick_search_path_6", length = 500)
    private String quickSearchPath6;

    @Column(name = "quick_search_path_7", length = 500)
    private String quickSearchPath7;

    @Column(name = "quick_search_path_8", length = 500)
    private String quickSearchPath8;

    @Column(name = "quick_search_path_9", length = 500)
    private String quickSearchPath9;

    @Column(name = "quick_search_path_10", length = 500)
    private String quickSearchPath10;

    /**
     * Optional human-readable labels describing each quick-search column, in the same order
     * as the quick-search path columns above.
     */
    @Column(name = "quick_search_label_1", length = 200)
    private String quickSearchLabel1;

    @Column(name = "quick_search_label_2", length = 200)
    private String quickSearchLabel2;

    @Column(name = "quick_search_label_3", length = 200)
    private String quickSearchLabel3;

    @Column(name = "quick_search_label_4", length = 200)
    private String quickSearchLabel4;

    @Column(name = "quick_search_label_5", length = 200)
    private String quickSearchLabel5;

    @Column(name = "quick_search_label_6", length = 200)
    private String quickSearchLabel6;

    @Column(name = "quick_search_label_7", length = 200)
    private String quickSearchLabel7;

    @Column(name = "quick_search_label_8", length = 200)
    private String quickSearchLabel8;

    @Column(name = "quick_search_label_9", length = 200)
    private String quickSearchLabel9;

    @Column(name = "quick_search_label_10", length = 200)
    private String quickSearchLabel10;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public Category() {
    }

    public Category(String name, String path, Category parent) {
        this.name = name;
        this.path = path;
        this.parent = parent;
    }

    public Category(String name, String path, Category parent, String createdBy, boolean isGlobal) {
        this.name = name;
        this.path = path;
        this.parent = parent;
        this.createdBy = createdBy;
        this.isGlobal = isGlobal;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Category getParent() {
        return parent;
    }

    public void setParent(Category parent) {
        this.parent = parent;
    }

    public List<Category> getChildren() {
        return children;
    }

    public void setChildren(List<Category> children) {
        this.children = children;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    public void setGlobal(boolean global) {
        isGlobal = global;
    }

    public List<CategoryShare> getSharedWith() {
        return sharedWith;
    }

    public void setSharedWith(List<CategoryShare> sharedWith) {
        this.sharedWith = sharedWith;
    }

    public JsonNode getJsonSchema() {
        return jsonSchema;
    }

    public void setJsonSchema(JsonNode jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    /**
     * Get the configured quick-search JSON Path expressions, in column order, skipping unset columns.
     */
    public List<String> getQuickSearchPaths() {
        return Stream.of(quickSearchPath1, quickSearchPath2, quickSearchPath3, quickSearchPath4, quickSearchPath5,
                quickSearchPath6, quickSearchPath7, quickSearchPath8, quickSearchPath9, quickSearchPath10)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Assign up to {@link #QUICK_SEARCH_COLUMN_COUNT} JSON Path expressions to the quick-search columns,
     * in order. Any remaining columns are cleared.
     */
    public void setQuickSearchPaths(List<String> paths) {
        String[] values = new String[QUICK_SEARCH_COLUMN_COUNT];
        if (paths != null) {
            for (int i = 0; i < Math.min(paths.size(), QUICK_SEARCH_COLUMN_COUNT); i++) {
                values[i] = paths.get(i);
            }
        }
        quickSearchPath1 = values[0];
        quickSearchPath2 = values[1];
        quickSearchPath3 = values[2];
        quickSearchPath4 = values[3];
        quickSearchPath5 = values[4];
        quickSearchPath6 = values[5];
        quickSearchPath7 = values[6];
        quickSearchPath8 = values[7];
        quickSearchPath9 = values[8];
        quickSearchPath10 = values[9];
    }

    /**
     * Get the configured quick-search labels, aligned by index with {@link #getQuickSearchPaths()}
     * (a null entry means that quick-search column has no label).
     */
    public List<String> getQuickSearchLabels() {
        int count = getQuickSearchPaths().size();
        return Stream.of(quickSearchLabel1, quickSearchLabel2, quickSearchLabel3, quickSearchLabel4, quickSearchLabel5,
                quickSearchLabel6, quickSearchLabel7, quickSearchLabel8, quickSearchLabel9, quickSearchLabel10)
                .collect(Collectors.toList())
                .subList(0, count);
    }

    /**
     * Assign up to {@link #QUICK_SEARCH_COLUMN_COUNT} labels to the quick-search columns, in order,
     * aligned by index with {@link #setQuickSearchPaths(List)}. Any remaining columns are cleared.
     */
    public void setQuickSearchLabels(List<String> labels) {
        String[] values = new String[QUICK_SEARCH_COLUMN_COUNT];
        if (labels != null) {
            for (int i = 0; i < Math.min(labels.size(), QUICK_SEARCH_COLUMN_COUNT); i++) {
                values[i] = labels.get(i);
            }
        }
        quickSearchLabel1 = values[0];
        quickSearchLabel2 = values[1];
        quickSearchLabel3 = values[2];
        quickSearchLabel4 = values[3];
        quickSearchLabel5 = values[4];
        quickSearchLabel6 = values[5];
        quickSearchLabel7 = values[6];
        quickSearchLabel8 = values[7];
        quickSearchLabel9 = values[8];
        quickSearchLabel10 = values[9];
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Helper method to add a child category
     */
    public void addChild(Category child) {
        children.add(child);
        child.setParent(this);
    }

    /**
     * Helper method to remove a child category
     */
    public void removeChild(Category child) {
        children.remove(child);
        child.setParent(null);
    }

    /**
     * Helper method to share category with a user
     */
    public void shareWith(String username, boolean canRead, boolean canWrite) {
        CategoryShare share = new CategoryShare(this, username, canRead, canWrite);
        sharedWith.add(share);
    }

    /**
     * Helper method to remove sharing for a user
     */
    public void unshareWith(String username) {
        sharedWith.removeIf(share -> share.getSharedWithUsername().equals(username));
    }

    /**
     * Check if category is accessible by a user
     */
    public boolean isAccessibleBy(String username) {
        if (isGlobal) return true;
        if (createdBy != null && createdBy.equals(username)) return true;
        return sharedWith.stream()
                .anyMatch(share -> share.getSharedWithUsername().equals(username) && share.isCanRead());
    }

    /**
     * Check if user has write permission
     */
    public boolean hasWritePermission(String username) {
        if (isGlobal) return false; // Global categories can only be modified by admins
        if (createdBy != null && createdBy.equals(username)) return true;
        return sharedWith.stream()
                .anyMatch(share -> share.getSharedWithUsername().equals(username) && share.isCanWrite());
    }
}
