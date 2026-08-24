package com.bridge.bdbank.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository JPA pour l'entité User.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Trouve un utilisateur par son nom d'utilisateur.
     */
    Optional<User> findByUsername(String username);

    /**
     * Vérifie si un nom d'utilisateur existe déjà.
     */
    boolean existsByUsername(String username);
}