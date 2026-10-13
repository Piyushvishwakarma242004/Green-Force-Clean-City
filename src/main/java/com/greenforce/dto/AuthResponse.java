package com.greenforce.dto;

public record AuthResponse(String token, String name, String email, String role) {
}
