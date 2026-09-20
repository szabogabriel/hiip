package com.hiip.datastorage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hiip.datastorage.entity.Category;
import com.hiip.datastorage.service.CategoryService.QuickSearchField;

import org.junit.jupiter.api.Test;

import java.util.List;

class CategoryServiceTest {

    private final CategoryService categoryService = new CategoryService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private JsonNode json(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---- extractQuickSearchFields ----

    @Test
    void extractQuickSearchFields_returnsEmptyForNullSchema() {
        assertThat(categoryService.extractQuickSearchFields(null)).isEmpty();
    }

    @Test
    void extractQuickSearchFields_returnsEmptyWhenKeywordMissing() {
        JsonNode schema = json("{\"type\":\"object\"}");

        assertThat(categoryService.extractQuickSearchFields(schema)).isEmpty();
    }

    @Test
    void extractQuickSearchFields_parsesPlainStringPathsWithNullLabels() {
        JsonNode schema = json("{\"x-quick-search\": [\"$.customerId\", \"$.status\"]}");

        List<QuickSearchField> fields = categoryService.extractQuickSearchFields(schema);

        assertThat(fields).containsExactly(
                new QuickSearchField("$.customerId", null),
                new QuickSearchField("$.status", null));
    }

    @Test
    void extractQuickSearchFields_parsesObjectEntriesWithLabels() {
        JsonNode schema = json(
                "{\"x-quick-search\": [{\"path\": \"$.status\", \"label\": \"status\"}, \"$.total\"]}");

        List<QuickSearchField> fields = categoryService.extractQuickSearchFields(schema);

        assertThat(fields).containsExactly(
                new QuickSearchField("$.status", "status"),
                new QuickSearchField("$.total", null));
    }

    @Test
    void extractQuickSearchFields_capsAtTenEntries() {
        StringBuilder arr = new StringBuilder("[");
        for (int i = 0; i < 15; i++) {
            if (i > 0) arr.append(",");
            arr.append("\"$.field").append(i).append("\"");
        }
        arr.append("]");
        JsonNode schema = json("{\"x-quick-search\": " + arr + "}");

        List<QuickSearchField> fields = categoryService.extractQuickSearchFields(schema);

        assertThat(fields).hasSize(Category.QUICK_SEARCH_COLUMN_COUNT);
    }

    @Test
    void extractQuickSearchFields_labelWithSpaceThrows() {
        JsonNode schema = json(
                "{\"x-quick-search\": [{\"path\": \"$.status\", \"label\": \"order status\"}]}");

        assertThatThrownBy(() -> categoryService.extractQuickSearchFields(schema))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("single word");
    }

    @Test
    void extractQuickSearchFields_nonArrayKeywordThrows() {
        JsonNode schema = json("{\"x-quick-search\": \"$.status\"}");

        assertThatThrownBy(() -> categoryService.extractQuickSearchFields(schema))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void extractQuickSearchFields_entryMissingPathThrows() {
        JsonNode schema = json("{\"x-quick-search\": [{\"label\": \"status\"}]}");

        assertThatThrownBy(() -> categoryService.extractQuickSearchFields(schema))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- validateSchemaDefinition ----

    @Test
    void validateSchemaDefinition_nullSchema_doesNotThrow() {
        categoryService.validateSchemaDefinition(null);
    }

    @Test
    void validateSchemaDefinition_validSchema_doesNotThrow() {
        JsonNode schema = json("{\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}}}");

        categoryService.validateSchemaDefinition(schema);
    }

    @Test
    void validateSchemaDefinition_invalidRegexPattern_throws() {
        JsonNode schema = json("{\"type\": \"string\", \"pattern\": \"[\"}");

        assertThatThrownBy(() -> categoryService.validateSchemaDefinition(schema))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- validateContentAgainstSchema ----

    @Test
    void validateContentAgainstSchema_categoryWithoutSchema_doesNotThrow() {
        Category category = new Category("orders", "orders", null, "alice", false);
        JsonNode content = json("{\"anything\": true}");

        categoryService.validateContentAgainstSchema(category, content);
    }

    @Test
    void validateContentAgainstSchema_matchingContent_doesNotThrow() {
        Category category = new Category("orders", "orders", null, "alice", false);
        category.setJsonSchema(json("{\"type\": \"object\", \"required\": [\"status\"]}"));

        categoryService.validateContentAgainstSchema(category, json("{\"status\": \"shipped\"}"));
    }

    @Test
    void validateContentAgainstSchema_violatingContent_throws() {
        Category category = new Category("orders", "orders", null, "alice", false);
        category.setJsonSchema(json("{\"type\": \"object\", \"required\": [\"status\"]}"));

        assertThatThrownBy(() -> categoryService.validateContentAgainstSchema(category, json("{}")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orders");
    }

    // ---- extractQuickSearchValues ----

    @Test
    void extractQuickSearchValues_resolvesConfiguredPaths() {
        Category category = new Category("orders", "orders", null, "alice", false);
        category.setQuickSearchPaths(List.of("$.status", "$.total"));

        List<String> values = categoryService.extractQuickSearchValues(category,
                json("{\"status\": \"shipped\", \"total\": 42.5}"));

        assertThat(values).containsExactly("shipped", "42.5");
    }

    @Test
    void extractQuickSearchValues_pathNotFoundYieldsNull() {
        Category category = new Category("orders", "orders", null, "alice", false);
        category.setQuickSearchPaths(List.of("$.status", "$.missing"));

        List<String> values = categoryService.extractQuickSearchValues(category,
                json("{\"status\": \"shipped\"}"));

        assertThat(values).containsExactly("shipped", null);
    }

    @Test
    void extractQuickSearchValues_noPathsConfigured_returnsEmpty() {
        Category category = new Category("orders", "orders", null, "alice", false);

        List<String> values = categoryService.extractQuickSearchValues(category, json("{\"status\": \"shipped\"}"));

        assertThat(values).isEmpty();
    }

    @Test
    void extractQuickSearchValues_nullCategoryOrContent_returnsEmpty() {
        assertThat(categoryService.extractQuickSearchValues(null, json("{}"))).isEmpty();

        Category category = new Category("orders", "orders", null, "alice", false);
        category.setQuickSearchPaths(List.of("$.status"));
        assertThat(categoryService.extractQuickSearchValues(category, null)).isEmpty();
    }
}
