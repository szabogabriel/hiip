package com.hiip.datastorage.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.hiip.datastorage.entity.DataStorage;
import java.time.LocalDateTime;
import java.util.Set;

public class DataStorageResponse {
    private Long id;
    private JsonNode content;
    private Set<String> tags;
    private String category;
    private String owner;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DataStorageResponse() {
    }

    public DataStorageResponse(DataStorage dataStorage) {
        this.id = dataStorage.getId();
        this.content = dataStorage.getContent();
        this.tags = dataStorage.getTags();
        this.category = dataStorage.getCategory() != null ? dataStorage.getCategory().getPath() : null;
        this.owner = dataStorage.getOwner();
        this.createdAt = dataStorage.getCreatedAt();
        this.updatedAt = dataStorage.getUpdatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public JsonNode getContent() {
        return content;
    }

    public void setContent(JsonNode content) {
        this.content = content;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
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
