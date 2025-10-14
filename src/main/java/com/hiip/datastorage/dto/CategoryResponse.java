package com.hiip.datastorage.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for Category information
 */
public class CategoryResponse {
    private Long id;
    private String name;
    private String path;
    private Long parentId;
    private String createdBy;
    private boolean isGlobal;
    private List<CategoryShareResponse> sharedWith;
    private List<CategoryResponse> children;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CategoryResponse() {
        this.children = new ArrayList<>();
        this.sharedWith = new ArrayList<>();
    }

    public CategoryResponse(Long id, String name, String path, Long parentId, String createdBy, 
                           boolean isGlobal, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.parentId = parentId;
        this.createdBy = createdBy;
        this.isGlobal = isGlobal;
        this.children = new ArrayList<>();
        this.sharedWith = new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
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

    public List<CategoryShareResponse> getSharedWith() {
        return sharedWith;
    }

    public void setSharedWith(List<CategoryShareResponse> sharedWith) {
        this.sharedWith = sharedWith;
    }

    public List<CategoryResponse> getChildren() {
        return children;
    }

    public void setChildren(List<CategoryResponse> children) {
        this.children = children;
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
}
