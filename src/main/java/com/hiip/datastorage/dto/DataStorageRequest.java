package com.hiip.datastorage.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;

public class DataStorageRequest {
    private JsonNode content;
    private Set<String> tags;

    public DataStorageRequest() {
    }

    public DataStorageRequest(JsonNode content, Set<String> tags) {
        this.content = content;
        this.tags = tags;
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
}
