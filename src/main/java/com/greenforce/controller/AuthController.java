package com.greenforce.controller;

import com.greenforce.dto.AuthResponse;
import com.greenforce.dto.FirebaseLoginRequest;
import com.greenforce.dto.LoginRequest;
import com.greenforce.dto.SignupRequest;
import com.greenforce.dto.UserResponse;
import com.greenforce.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Body: { "idToken": "<Firebase ID token from signInWithPopup>" } */
    @PostMapping("/firebase")
    public AuthResponse firebase(@Valid @RequestBody FirebaseLoginRequest request) {
        return authService.loginWithFirebase(request.idToken());
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return authService.me(authentication.getName());
    }
}
