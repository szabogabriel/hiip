package com.hiip.datastorage.dto;

public class UserRequest {

    private String username;
    private String password;
    private String email;
    private Boolean isAdmin = false;
    private Boolean isActive = true;

    public UserRequest() {
    }

    public UserRequest(String username, String password, String email, Boolean isAdmin, Boolean isActive) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.isAdmin = isAdmin;
        this.isActive = isActive;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
