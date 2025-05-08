package com.bitzomax.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private Long id;
    private String username;
    private String email;
    private String role;
    private boolean subscribed;
    
    public AuthResponse(String accessToken, Long id, String username, String email, String role, boolean subscribed) {
        this.accessToken = accessToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.subscribed = subscribed;
    }
}