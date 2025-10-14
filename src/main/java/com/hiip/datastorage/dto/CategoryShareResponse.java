package com.hiip.datastorage.dto;

import com.hiip.datastorage.entity.CategoryShare;
import java.time.LocalDateTime;

/**
 * Response DTO for category share information
 */
public class CategoryShareResponse {
    private Long id;
    private String sharedWithUsername;
    private boolean canRead;
    private boolean canWrite;
    private LocalDateTime sharedAt;

    public CategoryShareResponse() {
    }

    public CategoryShareResponse(CategoryShare share) {
        this.id = share.getId();
        this.sharedWithUsername = share.getSharedWithUsername();
        this.canRead = share.isCanRead();
        this.canWrite = share.isCanWrite();
        this.sharedAt = share.getSharedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSharedWithUsername() {
        return sharedWithUsername;
    }

    public void setSharedWithUsername(String sharedWithUsername) {
        this.sharedWithUsername = sharedWithUsername;
    }

    public boolean isCanRead() {
        return canRead;
    }

    public void setCanRead(boolean canRead) {
        this.canRead = canRead;
    }

    public boolean isCanWrite() {
        return canWrite;
    }

    public void setCanWrite(boolean canWrite) {
        this.canWrite = canWrite;
    }

    public LocalDateTime getSharedAt() {
        return sharedAt;
    }

    public void setSharedAt(LocalDateTime sharedAt) {
        this.sharedAt = sharedAt;
    }
}
