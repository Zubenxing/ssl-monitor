package com.sslmonitor.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;
    
    // 如果不使用Lombok，需要手动添加getter和setter
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
} 