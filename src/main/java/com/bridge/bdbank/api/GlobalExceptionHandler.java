package com.bridge.bdbank.api;

import com.bridge.bdbank.api.dto.ErrorResponse;
import com.bridge.bdbank.auth.AuthenticationException;
import com.bridge.bdbank.dsl.DslSyntaxException;
import com.bridge.bdbank.introspection.TableNotFoundException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Gestionnaire global des exceptions pour standardiser les réponses d'erreur.
 * Intercepte toutes les exceptions et retourne un format JSON cohérent.
 */
@RestControllerAdvice
@ApiResponses(value = {
    @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "Ressource non trouvée", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "Erreur interne du serveur", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class GlobalExceptionHandler {

    /**
     * Gère les exceptions d'authentification.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        
        ErrorResponse error = new ErrorResponse();
        error.setStatus(HttpStatus.UNAUTHORIZED.value());
        error.setMessage(ex.getMessage());
        error.setErrorType("AuthenticationException");
        error.setTimestamp(LocalDateTime.now());
        error.setPath(getPath(request));
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Gère les exceptions de parsing DSL.
     */
    @ExceptionHandler(DslSyntaxException.class)
    public ResponseEntity<ErrorResponse> handleDslSyntaxException(
            DslSyntaxException ex, WebRequest request) {
        
        ErrorResponse error = new ErrorResponse();
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setMessage("Erreur de syntaxe dans la règle DSL: " + ex.getMessage());
        error.setErrorType("DslSyntaxException");
        error.setTimestamp(LocalDateTime.now());
        error.setPath(getPath(request));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Gère les exceptions de table non trouvée.
     */
    @ExceptionHandler(TableNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTableNotFoundException(
            TableNotFoundException ex, WebRequest request) {
        
        ErrorResponse error = new ErrorResponse();
        error.setStatus(HttpStatus.NOT_FOUND.value());
        error.setMessage(ex.getMessage());
        error.setErrorType("TableNotFoundException");
        error.setTimestamp(LocalDateTime.now());
        error.setPath(getPath(request));
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Gère les exceptions d'arguments illégaux.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        ErrorResponse error = new ErrorResponse();
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setMessage(ex.getMessage());
        error.setErrorType("IllegalArgumentException");
        error.setTimestamp(LocalDateTime.now());
        error.setPath(getPath(request));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Gère toutes les autres exceptions non gérées.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        
        ErrorResponse error = new ErrorResponse();
        error.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.setMessage("Une erreur interne est survenue");
        error.setErrorType("InternalServerError");
        error.setTimestamp(LocalDateTime.now());
        error.setPath(getPath(request));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Extrait le chemin de la requête.
     */
    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}