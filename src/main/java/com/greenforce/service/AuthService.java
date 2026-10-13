package com.greenforce.service;

import com.greenforce.dto.AuthResponse;
import com.greenforce.dto.LoginRequest;
import com.greenforce.dto.SignupRequest;
import com.greenforce.dto.UserResponse;
import com.greenforce.entity.Role;
import com.greenforce.entity.User;
import com.greenforce.exception.ConflictException;
import com.greenforce.exception.UnauthorizedException;
import com.greenforce.repository.UserRepository;
import com.greenforce.security.FirebaseTokenVerifier;
import com.greenforce.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final FirebaseTokenVerifier firebase;

    public AuthService(UserRepository users, PasswordEncoder encoder,
                       JwtService jwtService, FirebaseTokenVerifier firebase) {
        this.users = users;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.firebase = firebase;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        // Public signup can only ever create normal users, never admins.
        User user = users.save(new User(request.name().trim(), email, encoder.encode(request.password()), Role.USER));
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmail(normalize(request.email()))
                .filter(u -> encoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        return toAuthResponse(user);
    }

    /** Google login: exchanges a verified Firebase ID token for our own JWT. */
    @Transactional
    public AuthResponse loginWithFirebase(String idToken) {
        Jwt token = firebase.verify(idToken);
        String email = token.getClaimAsString("email");
        if (email == null || !Boolean.TRUE.equals(token.getClaimAsBoolean("email_verified"))) {
            throw new UnauthorizedException("Google account email is not verified");
        }
        String normalizedEmail = normalize(email);
        String name = token.getClaimAsString("name");
        if (name == null || name.isBlank()) {
            name = normalizedEmail.substring(0, normalizedEmail.indexOf('@'));
        }
        final String displayName = name.length() > 100 ? name.substring(0, 100) : name;

        User user = users.findByEmail(normalizedEmail).orElseGet(() ->
                // random password: Google-created accounts cannot be used with password login
                users.save(new User(displayName, normalizedEmail,
                        encoder.encode(UUID.randomUUID().toString()), Role.USER)));
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(String email) {
        User user = users.findByEmail(email).orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(jwtService.generate(user), user.getName(), user.getEmail(), user.getRole().name());
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
