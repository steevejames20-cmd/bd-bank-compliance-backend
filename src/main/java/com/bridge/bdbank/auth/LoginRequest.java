package com.bridge.bdbank.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO pour la requête de connexion.
 */
public record LoginRequest(
    @NotBlank(message = "Le nom d'utilisateur est requis")
    String username,
    
    @NotBlank(message = "Le mot de passe est requis")
    String password
) {}