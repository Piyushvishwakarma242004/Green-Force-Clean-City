package com.greenforce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 150) String email,
        // 8 matches the check in home.html; 72 is BCrypt's input limit
        @NotBlank @Size(min = 8, max = 72) String password) {
}
