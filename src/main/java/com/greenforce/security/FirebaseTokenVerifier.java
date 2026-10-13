package com.greenforce.security;

import com.greenforce.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Verifies the ID token that Firebase gives Login.html after "Continue with Google"
 * (signature via Google's public keys, issuer, audience and expiry).
 * Keys are downloaded lazily on first use, so the app also starts offline.
 */
@Component
public class FirebaseTokenVerifier {

    private static final String JWK_SET_URI =
            "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

    private final JwtDecoder decoder;

    public FirebaseTokenVerifier(@Value("${app.firebase.project-id}") String projectId) {
        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI).build();

        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer("https://securetoken.google.com/" + projectId);
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
                "aud", aud -> aud != null && aud.contains(projectId));

        nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
        this.decoder = nimbus;
    }

    public Jwt verify(String idToken) {
        try {
            return decoder.decode(idToken);
        } catch (JwtException e) {
            throw new UnauthorizedException("Invalid or expired Google login token");
        }
    }
}
