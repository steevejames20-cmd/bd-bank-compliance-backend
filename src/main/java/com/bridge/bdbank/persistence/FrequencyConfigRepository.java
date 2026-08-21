package com.bridge.bdbank.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository JPA pour l'entité FrequencyConfig.
 * Gère la configuration de fréquence d'exécution des règles.
 */
@Repository
public interface FrequencyConfigRepository extends JpaRepository<FrequencyConfig, Long> {

    /**
     * Trouve la configuration de fréquence active.
     * Il ne devrait y avoir qu'une seule configuration active à la fois.
     */
    Optional<FrequencyConfig> findByActiveTrue();

    /**
     * Vérifie si une configuration active existe.
     */
    boolean existsByActiveTrue();
}