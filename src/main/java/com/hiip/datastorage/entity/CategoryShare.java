package com.hiip.datastorage.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing the sharing relationship between categories and users.
 * Defines permissions for shared categories.
 */
@Entity
@Table(name = "category_shares", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"category_id", "shared_with_username"})
})
public class CategoryShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * Username of the user this category is shared with
     */
    @Column(name = "shared_with_username", nullable = false, length = 100)
    private String sharedWithUsername;

    /**
     * Can the shared user read data in this category?
     */
    @Column(nullable = false)
    private boolean canRead = true;

    /**
     * Can the shared user create/update/delete data in this category?
     */
    @Column(nullable = false)
    private boolean canWrite = false;

    @Column(nullable = false)
    private LocalDateTime sharedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        sharedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public CategoryShare() {
    }

    public CategoryShare(Category category, String sharedWithUsername, boolean canRead, boolean canWrite) {
        this.category = category;
        this.sharedWithUsername = sharedWithUsername;
        this.canRead = canRead;
        this.canWrite = canWrite;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
