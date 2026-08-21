package com.bridge.bdbank.auth;

import com.bridge.bdbank.persistence.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour l'authentification.
 * Gère les endpoints de login, logout et récupération des infos utilisateur.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    /**
     * Endpoint de connexion.
     * POST /auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            String token = authenticationService.login(request.username(), request.password());
            User user = authenticationService.getUserInfo(token);
            
            LoginResponse response = new LoginResponse(
                token,
                user.getUsername(),
                user.getRole()
            );
            
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Endpoint de déconnexion.
     * POST /auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractToken(authHeader);
        if (token != null) {
            authenticationService.logout(token);
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Endpoint pour récupérer les informations de l'utilisateur connecté.
     * GET /auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String token = extractToken(authHeader);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            User user = authenticationService.getUserInfo(token);
            return ResponseEntity.ok(user);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Extrait le token du header Authorization.
     * Format attendu: "Bearer <token>"
     */
    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7); // "Bearer ".length() == 7
    }
}