package com.bridge.bdbank.auth;

/**
 * DTO pour la réponse de connexion.
 */
public record LoginResponse(
    String token,
    String username,
    String role
) {}