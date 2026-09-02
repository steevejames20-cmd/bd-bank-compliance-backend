package com.bridge.bdbank.api;

import com.bridge.bdbank.api.dto.LoginRequest;
import com.bridge.bdbank.api.dto.LoginResponse;
import com.bridge.bdbank.auth.AuthenticationException;
import com.bridge.bdbank.auth.AuthenticationService;
import com.bridge.bdbank.persistence.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    @Value("${bdbank.setup-key:}")
    private String setupKey;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authenticationService.login(request.getUsername(), request.getPassword());
        User user = authenticationService.getUserInfo(token);
        return ResponseEntity.ok(LoginResponse.builder()
            .token(token)
            .userId(user.getId())
            .username(user.getUsername())
            .role(user.getRole())
            .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        authenticationService.logout(extractToken(authHeader));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponse> me(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractToken(authHeader);
        User user = authenticationService.getUserInfo(token);
        return ResponseEntity.ok(LoginResponse.builder()
            .token(token)
            .userId(user.getId())
            .username(user.getUsername())
            .role(user.getRole())
            .build());
    }

    @PostMapping("/setup")
    public ResponseEntity<Void> setup(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Setup-Key", required = false) String requestKey) {
        if (setupKey.isBlank() || requestKey == null || !setupKey.equals(requestKey)) {
            throw new AuthenticationException("Initialisation non autorisée");
        }
        authenticationService.createInitialUser(request.getUsername(), request.getPassword());
        return ResponseEntity.status(201).build();
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AuthenticationException("Token manquant");
        }
        return authHeader.substring(7);
    }
}