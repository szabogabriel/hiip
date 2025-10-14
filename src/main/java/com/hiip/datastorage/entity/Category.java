package com.hiip.datastorage.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
