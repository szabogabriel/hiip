package com.hiip.datastorage.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;

public class DataStorageRequest {
    private JsonNode content;
    private Set<String> tags;
    private String category;

    public DataStorageRequest() {
    }

    public DataStorageRequest(JsonNode content, Set<String> tags, String category) {
        this.content = content;
        this.tags = tags;
        this.category = category;
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
}
