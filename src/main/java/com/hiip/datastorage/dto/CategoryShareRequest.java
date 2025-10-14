package com.hiip.datastorage.dto;

/**
 * Request DTO for sharing a category with users
 */
public class CategoryShareRequest {
    private String username;
    private boolean canRead = true;
    private boolean canWrite = false;

    public CategoryShareRequest() {
    }

    public CategoryShareRequest(String username, boolean canRead, boolean canWrite) {
        this.username = username;
        this.canRead = canRead;
        this.canWrite = canWrite;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
}
