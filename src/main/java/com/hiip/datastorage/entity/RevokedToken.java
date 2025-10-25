package com.hiip.datastorage.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a revoked JWT token (blacklist).
 * Used to invalidate tokens before their natural expiration.
 */
@Entity
@Table(name = "revoked_tokens", indexes = {
    @Index(name = "idx_token", columnList = "token"),
    @Index(name = "idx_expiration", columnList = "expirationDate")
})
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private LocalDateTime revokedAt;

    @Column(nullable = false)
    private LocalDateTime expirationDate;

    // Constructors
    public RevokedToken() {
    }

    public RevokedToken(String token, String username, LocalDateTime expirationDate) {
        this.token = token;
        this.username = username;
        this.revokedAt = LocalDateTime.now();
        this.expirationDate = expirationDate;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }
}
