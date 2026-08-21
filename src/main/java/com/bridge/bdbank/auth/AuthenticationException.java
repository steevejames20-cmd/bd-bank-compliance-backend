package com.bridge.bdbank.auth;

/**
 * Exception levée lors d'erreurs d'authentification.
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}