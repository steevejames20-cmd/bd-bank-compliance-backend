package com.bridge.bdbank.auth;

import com.bridge.bdbank.persistence.User;
import com.bridge.bdbank.persistence.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Instant;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Service d'authentification simplifié pour J17.
 * Implémente la validation basique des tokens et le blocage après 5 échecs.
 */
@Service
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;
    private static final Duration SESSION_IDLE_TIMEOUT = Duration.ofMinutes(15);
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{3,50}$");
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    public AuthenticationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Valide un token de session.
     * Pour l'instant, version simplifiée qui vérifie juste le format.
     */
    public User validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new AuthenticationException("Token invalide");
        }

        Session session = sessions.get(token);
        if (session == null || session.isExpired()) {
            sessions.remove(token);
            throw new AuthenticationException("Session expirée ou invalide");
        }
        session.touch();
        return session.user;
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
        
        String token = UUID.randomUUID().toString();
        sessions.put(token, new Session(user, Instant.now()));
        return token;
    }

    /**
     * Crée le premier compte administrateur. Cette opération est volontairement
     * disponible uniquement via le parcours d'initialisation protégé.
     */
    public synchronized void createInitialUser(String username, String password) {
        if (userRepository.count() > 0) {
            throw new AuthenticationException("Le compte administrateur existe déjà");
        }
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("L'identifiant doit contenir 3 à 50 caractères alphanumériques");
        }
        if (password == null || password.length() < 12) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 12 caractères");
        }

        LocalDateTime now = LocalDateTime.now();
        userRepository.save(User.builder()
            .username(username)
            .passwordHash(passwordEncoder.encode(password))
            .role("ADMIN")
            .active(true)
            .failedLoginAttempts(0)
            .createdAt(now)
            .updatedAt(now)
            .build());
    }

    /**
     * Déconnecte un utilisateur (version simplifiée).
     */
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            throw new AuthenticationException("Token manquant");
        }
        sessions.remove(token);
        log.info("Logout pour le token: {}", token);
    }

    /**
     * Récupère les informations de l'utilisateur (version simplifiée).
     */
    public User getUserInfo(String token) {
        return validateToken(token);
    }

    private static final class Session {
        private final User user;
        private volatile Instant lastActivity;

        private Session(User user, Instant lastActivity) {
            this.user = user;
            this.lastActivity = lastActivity;
        }

        private boolean isExpired() {
            return lastActivity.plus(SESSION_IDLE_TIMEOUT).isBefore(Instant.now());
        }

        private void touch() {
            lastActivity = Instant.now();
        }
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