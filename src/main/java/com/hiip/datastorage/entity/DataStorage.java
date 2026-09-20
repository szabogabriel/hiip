package com.hiip.datastorage.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "data_storage")
public class DataStorage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode content;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "data_tags", joinColumns = @JoinColumn(name = "data_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    /**
     * Values extracted from {@link #content} using the owning category's quick-search JSON Paths,
     * used for indexed lookups without needing to parse the full content.
     */
    @Column(name = "quick_search_1", length = 500)
    private String quickSearch1;

    @Column(name = "quick_search_2", length = 500)
    private String quickSearch2;

    @Column(name = "quick_search_3", length = 500)
    private String quickSearch3;

    @Column(name = "quick_search_4", length = 500)
    private String quickSearch4;

    @Column(name = "quick_search_5", length = 500)
    private String quickSearch5;

    @Column(name = "quick_search_6", length = 500)
    private String quickSearch6;

    @Column(name = "quick_search_7", length = 500)
    private String quickSearch7;

    @Column(name = "quick_search_8", length = 500)
    private String quickSearch8;

    @Column(name = "quick_search_9", length = 500)
    private String quickSearch9;

    @Column(name = "quick_search_10", length = 500)
    private String quickSearch10;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private boolean hidden = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public DataStorage() {
    }

    public DataStorage(JsonNode content, Set<String> tags, String owner, Category category) {
        this.content = content;
        this.tags = tags != null ? tags : new HashSet<>();
        this.owner = owner;
        this.category = category;
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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    /**
     * Get the quick-search values, in column order, skipping unset columns.
     */
    public List<String> getQuickSearchValues() {
        return Stream.of(quickSearch1, quickSearch2, quickSearch3, quickSearch4, quickSearch5,
                quickSearch6, quickSearch7, quickSearch8, quickSearch9, quickSearch10)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Assign up to {@link Category#QUICK_SEARCH_COLUMN_COUNT} values to the quick-search columns,
     * in order. Any remaining columns are cleared.
     */
    public void setQuickSearchValues(List<String> values) {
        String[] columns = new String[Category.QUICK_SEARCH_COLUMN_COUNT];
        if (values != null) {
            for (int i = 0; i < Math.min(values.size(), Category.QUICK_SEARCH_COLUMN_COUNT); i++) {
                columns[i] = values.get(i);
            }
        }
        quickSearch1 = columns[0];
        quickSearch2 = columns[1];
        quickSearch3 = columns[2];
        quickSearch4 = columns[3];
        quickSearch5 = columns[4];
        quickSearch6 = columns[5];
        quickSearch7 = columns[6];
        quickSearch8 = columns[7];
        quickSearch9 = columns[8];
        quickSearch10 = columns[9];
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
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
