package com.bridge.bdbank.auth;

import com.bridge.bdbank.persistence.User;
import com.bridge.bdbank.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service d'authentification simplifié pour J17.
 * Implémente la validation basique des tokens et le blocage après 5 échecs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    /**
     * Valide un token de session.
     * Pour l'instant, version simplifiée qui vérifie juste le format.
     */
    public User validateToken(String token) {
        // Version simplifiée : vérifie que le token n'est pas null/empty
        if (token == null || token.trim().isEmpty()) {
            throw new AuthenticationException("Token invalide");
        }
        
        // Pour le moment, on retourne un utilisateur par défaut
        // Le système complet sera implémenté avec les entités User/UserSession
        return User.builder()
            .id(1L)
            .username("admin")
            .role("ADMIN")
            .build();
    }

    /**
     * Authentifie un utilisateur avec gestion du blocage après 5 échecs.
     */
    public String login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            throw new AuthenticationException("Identifiants invalides");
        }
        
        User user = userOpt.get();
        
        // Vérifier si le compte est verrouillé
        if (user.getLockedUntil() != null && LocalDateTime.now().isBefore(user.getLockedUntil())) {
            throw new AuthenticationException("Compte temporairement verrouillé. Réessayez plus tard.");
        }
        
        // Vérifier le mot de passe
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new AuthenticationException("Identifiants invalides");
        }
        
        // Réinitialiser les tentatives échouées en cas de succès
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
        
        // Version simplifiée : génère un token basique
        return "token-" + System.currentTimeMillis();
    }

    /**
     * Déconnecte un utilisateur (version simplifiée).
     */
    public void logout(String token) {
        log.info("Logout pour le token: {}", token);
    }

    /**
     * Récupère les informations de l'utilisateur (version simplifiée).
     */
    public User getUserInfo(String token) {
        return validateToken(token);
    }

    /**
     * Gère une tentative de connexion échouée avec blocage après 5 échecs.
     */
    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            log.warn("Compte {} verrouillé après {} tentatives échouées", user.getUsername(), attempts);
        } else {
            log.warn("Tentative de connexion échouée {} pour l'utilisateur {}", attempts, user.getUsername());
        }

        userRepository.save(user);
    }
}