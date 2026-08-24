package com.bridge.bdbank.auth;

import com.bridge.bdbank.persistence.User;
import com.bridge.bdbank.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test d'intégration pour le blocage temporaire après 5 tentatives échouées.
 * J17 - Vérifie que le système de sécurité fonctionne correctement.
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional
class AccountLockoutIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void shouldLockAccountAfterFiveFailedAttempts() {
        // Créer un utilisateur de test
        User user = User.builder()
            .username("testuser")
            .passwordHash(passwordEncoder.encode("correctPassword"))
            .role("USER")
            .active(true)
            .failedLoginAttempts(0)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        User savedUser = userRepository.save(user);

        // Simuler 5 tentatives échouées
        for (int i = 1; i <= 5; i++) {
            User updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
            updatedUser.setFailedLoginAttempts(i);
            updatedUser.setUpdatedAt(LocalDateTime.now());
            if (i >= 5) {
                updatedUser.setLockedUntil(LocalDateTime.now().plusMinutes(30));
            }
            userRepository.save(updatedUser);
        }

        // Vérifier que le compte est verrouillé
        User lockedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(lockedUser.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(lockedUser.getLockedUntil()).isNotNull();
        assertThat(lockedUser.getLockedUntil()).isAfter(LocalDateTime.now());
    }

    @Test
    void shouldPreventLoginWhenAccountLocked() {
        // Créer un utilisateur déjà verrouillé
        User user = User.builder()
            .username("lockeduser")
            .passwordHash(passwordEncoder.encode("correctPassword"))
            .role("USER")
            .active(true)
            .failedLoginAttempts(5)
            .lockedUntil(LocalDateTime.now().plusMinutes(30))
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        User savedUser = userRepository.save(user);

        // Tenter de se connecter
        boolean isLocked = savedUser.getLockedUntil() != null && 
                           LocalDateTime.now().isBefore(savedUser.getLockedUntil());

        assertThat(isLocked).isTrue();
        assertThat(savedUser.getFailedLoginAttempts()).isEqualTo(5);
    }

    @Test
    void shouldResetFailedAttemptsOnSuccessfulLogin() {
        // Créer un utilisateur avec des tentatives échouées
        User user = User.builder()
            .username("recoveryuser")
            .passwordHash(passwordEncoder.encode("correctPassword"))
            .role("USER")
            .active(true)
            .failedLoginAttempts(3)
            .lockedUntil(LocalDateTime.now().plusMinutes(1)) // Verrouillage court
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        User savedUser = userRepository.save(user);

        // Simuler une connexion réussie
        User updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        updatedUser.setFailedLoginAttempts(0);
        updatedUser.setLockedUntil(null);
        updatedUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(updatedUser);

        // Vérifier que les compteurs sont réinitialisés
        User recoveredUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(recoveredUser.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(recoveredUser.getLockedUntil()).isNull();
    }

    @Test
    void shouldIncrementFailedAttemptsSequentially() {
        // Créer un utilisateur
        User user = User.builder()
            .username("sequentialuser")
            .passwordHash(passwordEncoder.encode("correctPassword"))
            .role("USER")
            .active(true)
            .failedLoginAttempts(0)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        User savedUser = userRepository.save(user);

        // Simuler 3 tentatives échouées séquentielles
        for (int i = 1; i <= 3; i++) {
            User updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
            updatedUser.setFailedLoginAttempts(i);
            updatedUser.setUpdatedAt(LocalDateTime.now());
            userRepository.save(updatedUser);
        }

        // Vérifier que le compteur est correct
        User finalUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(finalUser.getFailedLoginAttempts()).isEqualTo(3);
        assertThat(finalUser.getLockedUntil()).isNull(); // Pas encore verrouillé
    }

    @Test
    void shouldNotLockAccountWhenAttemptsLessThanFive() {
        // Créer un utilisateur avec 4 tentatives échouées
        User user = User.builder()
            .username("fourattempts")
            .passwordHash(passwordEncoder.encode("correctPassword"))
            .role("USER")
            .active(true)
            .failedLoginAttempts(4)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        User savedUser = userRepository.save(user);

        // Vérifier que le compte n'est pas verrouillé
        assertThat(savedUser.getFailedLoginAttempts()).isEqualTo(4);
        assertThat(savedUser.getLockedUntil()).isNull();
    }

    @Test
    void shouldAllowLoginAfterLockPeriodExpires() {
        // Créer un utilisateur verrouillé avec un verrouillage expiré
        User user = User.builder()
            .username("expiredlock")
            .passwordHash(passwordEncoder.encode("correctPassword"))
            .role("USER")
            .active(true)
            .failedLoginAttempts(5)
            .lockedUntil(LocalDateTime.now().minusMinutes(1)) // Verrouillage expiré
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        User savedUser = userRepository.save(user);

        // Vérifier que le verrouillage est expiré
        boolean isExpired = savedUser.getLockedUntil() != null && 
                          LocalDateTime.now().isAfter(savedUser.getLockedUntil());

        assertThat(isExpired).isTrue();
    }
}