package com.hiip.datastorage.dto;

import com.hiip.datastorage.entity.DataStorage;
import java.time.LocalDateTime;
import java.util.Set;

public class DataStorageResponse {
    private Long id;
    private String content;
    private Set<String> tags;
    private String owner;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DataStorageResponse() {
    }

    public DataStorageResponse(DataStorage dataStorage) {
        this.id = dataStorage.getId();
        this.content = dataStorage.getContent();
        this.tags = dataStorage.getTags();
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
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
