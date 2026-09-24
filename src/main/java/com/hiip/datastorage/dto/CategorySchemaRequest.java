package com.hiip.datastorage.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Request DTO for updating a category's JSON schema
 */
public class CategorySchemaRequest {
    private JsonNode schema;

    public CategorySchemaRequest() {
    }

    public CategorySchemaRequest(JsonNode schema) {
        this.schema = schema;
    }

    public JsonNode getSchema() {
        return schema;
    }

    public void setSchema(JsonNode schema) {
        this.schema = schema;
    }
}
