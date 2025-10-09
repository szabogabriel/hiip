package com.hiip.datastorage.dto;

import java.util.Set;

public class DataStorageRequest {
    private String content;
    private Set<String> tags;

    public DataStorageRequest() {
    }

    public DataStorageRequest(String content, Set<String> tags) {
        this.content = content;
        this.tags = tags;
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
}
