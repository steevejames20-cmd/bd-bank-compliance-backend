package com.bridge.bdbank.auth;

import com.bridge.bdbank.persistence.User;
import com.bridge.bdbank.persistence.UserRepository;
import com.bridge.bdbank.persistence.UserSession;
import com.bridge.bdbank.persistence.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Service d'authentification gérant les utilisateurs et les sessions.
 * Implémente le login, logout, et la gestion des tokens de session.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;
    private static final int SESSION_DURATION_MINUTES = 15;

    /**
     * Authentifie un utilisateur et crée une session.
     *
     * @param username Nom d'utilisateur
     * @param password Mot de passe en clair
     * @return Token de session si authentification réussie
     * @throws AuthenticationException si l'authentification échoue
     */
    @Transactional
    public String login(String username, String password) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new AuthenticationException("Identifiants invalides"));

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

        // Créer une nouvelle session
        String token = generateToken();
        UserSession session = UserSession.builder()
            .token(token)
            .user(user)
            .expiresAt(LocalDateTime.now().plusMinutes(SESSION_DURATION_MINUTES))
            .lastActivityAt(LocalDateTime.now())
            .build();

        sessionRepository.save(session);
        log.info("Utilisateur {} connecté avec succès", username);

        return token;
    }

    /**
     * Déconnecte un utilisateur en invalidant sa session.
     *
     * @param token Token de session
     */
    @Transactional
    public void logout(String token) {
        sessionRepository.findByToken(token).ifPresent(session -> {
            sessionRepository.delete(session);
            log.info("Session invalidée pour l'utilisateur {}", session.getUser().getUsername());
        });
    }

    /**
     * Valide un token de session et met à jour l'activité.
     *
     * @param token Token de session
     * @return Utilisateur si le token est valide
     * @throws AuthenticationException si le token est invalide ou expiré
     */
    @Transactional
    public User validateToken(String token) {
        Optional<UserSession> sessionOpt = sessionRepository.findByToken(token);

        if (sessionOpt.isEmpty()) {
            throw new AuthenticationException("Token invalide");
        }

        UserSession session = sessionOpt.get();

        // Vérifier l'expiration
        if (session.isExpired()) {
            sessionRepository.delete(session);
            throw new AuthenticationException("Session expirée");
        }

        // Vérifier l'inactivité (15 minutes)
        if (session.isInactive()) {
            sessionRepository.delete(session);
            throw new AuthenticationException("Session inactive");
        }

        // Mettre à jour la dernière activité
        session.setLastActivityAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusMinutes(SESSION_DURATION_MINUTES));
        sessionRepository.save(session);

        return session.getUser();
    }

    /**
     * Récupère les informations de l'utilisateur connecté.
     *
     * @param token Token de session
     * @return Informations de l'utilisateur
     */
    public User getUserInfo(String token) {
        return validateToken(token);
    }

    /**
     * Gère une tentative de connexion échouée.
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

    /**
     * Génère un token de session sécurisé.
     */
    private String generateToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Nettoie les sessions expirées et inactives.
     */
    @Transactional
    public void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime inactiveThreshold = now.minusMinutes(SESSION_DURATION_MINUTES);

        sessionRepository.deleteExpiredSessions(now);
        sessionRepository.deleteInactiveSessions(inactiveThreshold);

        log.debug("Nettoyage des sessions expirées et inactives terminé");
    }
}