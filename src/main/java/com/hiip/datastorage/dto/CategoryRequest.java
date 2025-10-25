package com.hiip.datastorage.dto;

/**
 * Request DTO for creating a category
 */
public class CategoryRequest {
    private String name;
    private String path;
    private Long parentId;
    private boolean isGlobal = false;

    public CategoryRequest() {
    }

    public CategoryRequest(String name, String path, Long parentId, boolean isGlobal) {
        this.name = name;
        this.path = path;
        this.parentId = parentId;
        this.isGlobal = isGlobal;
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

    public boolean isGlobal() {
        return isGlobal;
    }

    public void setGlobal(boolean global) {
        isGlobal = global;
    }
}
