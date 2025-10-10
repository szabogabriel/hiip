package com.hiip.datastorage.dto;

public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private Boolean isAdmin;
    private Boolean isActive;

    public UserResponse() {
    }

    public UserResponse(Long id, String username, String email, Boolean isAdmin, Boolean isActive) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.isAdmin = isAdmin;
        this.isActive = isActive;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
