package com.hiip.datastorage.dto;

public class PasswordResetRequest {
    private String usernameOrEmail;
    
    public PasswordResetRequest() {}
    
    public PasswordResetRequest(String usernameOrEmail) {
        this.usernameOrEmail = usernameOrEmail;
    }
    
    public String getUsernameOrEmail() {
        return usernameOrEmail;
    }
    
    public void setUsernameOrEmail(String usernameOrEmail) {
        this.usernameOrEmail = usernameOrEmail;
    }
}