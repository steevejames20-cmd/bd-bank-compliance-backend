package com.bridge.bdbank.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour l'entité UserSession.
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    /**
     * Trouve une session par son token.
     */
    Optional<UserSession> findByToken(String token);

    /**
     * Trouve toutes les sessions actives pour un utilisateur.
     */
    List<UserSession> findByUserIdAndExpiresAtAfter(Long userId, LocalDateTime now);

    /**
     * Supprime toutes les sessions expirées.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :now")
    void deleteExpiredSessions(@Param("now") LocalDateTime now);

    /**
     * Supprime toutes les sessions inactives (plus de 15 minutes).
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserSession s WHERE s.lastActivityAt < :inactiveThreshold")
    void deleteInactiveSessions(@Param("inactiveThreshold") LocalDateTime inactiveThreshold);

    /**
     * Met à jour la dernière activité d'une session.
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserSession s SET s.lastActivityAt = :now WHERE s.token = :token")
    void updateLastActivity(@Param("token") String token, @Param("now") LocalDateTime now);
}