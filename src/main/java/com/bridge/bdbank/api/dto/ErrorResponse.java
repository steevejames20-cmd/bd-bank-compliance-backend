package com.bridge.bdbank.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO standardisé pour les réponses d'erreur.
 * Fournit un format cohérent pour toutes les erreurs de l'API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    /**
     * Code HTTP de l'erreur
     */
    private int status;
    
    /**
     * Message d'erreur lisible par l'utilisateur
     */
    private String message;
    
    /**
     * Type d'erreur (ex: "AuthenticationException", "ValidationException")
     */
    private String errorType;
    
    /**
     * Timestamp de l'erreur
     */
    private LocalDateTime timestamp;
    
    /**
     * Liste des détails d'erreur spécifiques (ex: champs de validation invalides)
     */
    private List<String> details;
    
    /**
     * Chemin de la requête qui a généré l'erreur
     */
    private String path;
}