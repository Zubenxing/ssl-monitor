package com.sslmonitor.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private String token;
    
    public AuthResponse() {
    }
    
    public AuthResponse(String token) {
        this.token = token;
    }
    
    // 如果不使用Lombok，需要手动添加getter和setter
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
} 