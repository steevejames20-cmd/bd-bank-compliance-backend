package com.bridge.bdbank.execution;

import com.bridge.bdbank.persistence.FrequencyConfig;
import com.bridge.bdbank.persistence.FrequencyConfigRepository;
import com.bridge.bdbank.persistence.FrequencyConfig.FrequencyType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service pour la gestion de la configuration de fréquence d'exécution des règles.
 * Gère la création, la modification et la validation des configurations de scheduler.
 */
@Service
@RequiredArgsConstructor
public class FrequencyConfigService {

    private static final Logger log = LoggerFactory.getLogger(FrequencyConfigService.class);
    private static final int MINIMUM_INTERVAL_MINUTES = 3;

    private final FrequencyConfigRepository frequencyConfigRepository;

    /**
     * Récupère la configuration de fréquence active.
     * 
     * @return La configuration active ou Optional.empty() si aucune n'est active
     */
    public Optional<FrequencyConfig> getActiveConfig() {
        return frequencyConfigRepository.findByActiveTrue();
    }

    /**
     * Récupère toutes les configurations de fréquence.
     * 
     * @return La liste de toutes les configurations
     */
    public List<FrequencyConfig> getAllConfigs() {
        return frequencyConfigRepository.findAll();
    }

    /**
     * Crée une nouvelle configuration de fréquence.
     * Désactive automatiquement toute autre configuration active.
     * 
     * @param config La configuration à créer
     * @return La configuration créée
     * @throws IllegalArgumentException si la configuration est invalide
     */
    @Transactional
    public FrequencyConfig createConfig(FrequencyConfig config) {
        validateConfig(config);
        
        // Désactiver toute autre configuration active
        frequencyConfigRepository.findByActiveTrue().ifPresent(existing -> {
            existing.setActive(false);
            frequencyConfigRepository.save(existing);
            log.info("Configuration précédente (id: {}) désactivée", existing.getId());
        });

        FrequencyConfig saved = frequencyConfigRepository.save(config);
        log.info("Nouvelle configuration de fréquence créée (id: {}, type: {})", 
            saved.getId(), saved.getType());
        return saved;
    }

    /**
     * Met à jour une configuration existante.
     * 
     * @param id L'identifiant de la configuration
     * @param config Les nouvelles données de configuration
     * @return La configuration mise à jour
     * @throws IllegalArgumentException si la configuration n'existe pas ou est invalide
     */
    @Transactional
    public FrequencyConfig updateConfig(Long id, FrequencyConfig config) {
        FrequencyConfig existing = frequencyConfigRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Configuration non trouvée avec l'ID: " + id));

        validateConfig(config);

        // Si on active cette configuration, désactiver les autres
        if (config.getActive() && !existing.getActive()) {
            frequencyConfigRepository.findByActiveTrue().ifPresent(other -> {
                if (!other.getId().equals(id)) {
                    other.setActive(false);
                    frequencyConfigRepository.save(other);
                    log.info("Configuration précédente (id: {}) désactivée", other.getId());
                }
            });
        }

        // Mettre à jour les champs
        existing.setType(config.getType());
        existing.setIntervalMinutes(config.getIntervalMinutes());
        existing.setCronExpression(config.getCronExpression());
        existing.setActive(config.getActive());

        FrequencyConfig saved = frequencyConfigRepository.save(existing);
        log.info("Configuration de fréquence mise à jour (id: {})", id);
        return saved;
    }

    /**
     * Active une configuration spécifique.
     * Désactive automatiquement toute autre configuration active.
     * 
     * @param id L'identifiant de la configuration à activer
     * @return La configuration activée
     * @throws IllegalArgumentException si la configuration n'existe pas
     */
    @Transactional
    public FrequencyConfig activateConfig(Long id) {
        FrequencyConfig config = frequencyConfigRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Configuration non trouvée avec l'ID: " + id));

        // Désactiver toute autre configuration active
        frequencyConfigRepository.findByActiveTrue().ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                existing.setActive(false);
                frequencyConfigRepository.save(existing);
                log.info("Configuration précédente (id: {}) désactivée", existing.getId());
            }
        });

        config.setActive(true);
        FrequencyConfig saved = frequencyConfigRepository.save(config);
        log.info("Configuration de fréquence activée (id: {})", id);
        return saved;
    }

    /**
     * Désactive une configuration spécifique.
     * 
     * @param id L'identifiant de la configuration à désactiver
     * @return La configuration désactivée
     * @throws IllegalArgumentException si la configuration n'existe pas
     */
    @Transactional
    public FrequencyConfig deactivateConfig(Long id) {
        FrequencyConfig config = frequencyConfigRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Configuration non trouvée avec l'ID: " + id));

        config.setActive(false);
        FrequencyConfig saved = frequencyConfigRepository.save(config);
        log.info("Configuration de fréquence désactivée (id: {})", id);
        return saved;
    }

    /**
     * Supprime une configuration.
     * 
     * @param id L'identifiant de la configuration à supprimer
     * @throws IllegalArgumentException si la configuration n'existe pas
     */
    @Transactional
    public void deleteConfig(Long id) {
        if (!frequencyConfigRepository.existsById(id)) {
            throw new IllegalArgumentException("Configuration non trouvée avec l'ID: " + id);
        }
        frequencyConfigRepository.deleteById(id);
        log.info("Configuration de fréquence supprimée (id: {})", id);
    }

    /**
     * Met à jour la date de dernière exécution pour la configuration active.
     */
    @Transactional
    public void updateLastExecution() {
        frequencyConfigRepository.findByActiveTrue().ifPresent(config -> {
            config.setLastExecutionAt(LocalDateTime.now());
            frequencyConfigRepository.save(config);
            log.debug("Date de dernière exécution mise à jour pour la configuration (id: {})", config.getId());
        });
    }

    /**
     * Valide une configuration de fréquence.
     * 
     * @param config La configuration à valider
     * @throws IllegalArgumentException si la configuration est invalide
     */
    private void validateConfig(FrequencyConfig config) {
        if (config.getType() == null) {
            throw new IllegalArgumentException("Le type de fréquence est obligatoire");
        }

        switch (config.getType()) {
            case INTERVAL:
                if (config.getIntervalMinutes() == null) {
                    throw new IllegalArgumentException("L'intervalle en minutes est obligatoire pour le type INTERVAL");
                }
                if (config.getIntervalMinutes() < MINIMUM_INTERVAL_MINUTES) {
                    throw new IllegalArgumentException(
                        String.format("L'intervalle minimum est de %d minutes (valeur fournie: %d)", 
                            MINIMUM_INTERVAL_MINUTES, config.getIntervalMinutes()));
                }
                if (config.getCronExpression() != null) {
                    throw new IllegalArgumentException("L'expression cron ne doit pas être définie pour le type INTERVAL");
                }
                break;

            case CRON:
                if (config.getCronExpression() == null || config.getCronExpression().trim().isEmpty()) {
                    throw new IllegalArgumentException("L'expression cron est obligatoire pour le type CRON");
                }
                if (!isValidCronExpression(config.getCronExpression())) {
                    throw new IllegalArgumentException("L'expression cron n'est pas valide: " + config.getCronExpression());
                }
                if (config.getIntervalMinutes() != null) {
                    throw new IllegalArgumentException("L'intervalle en minutes ne doit pas être défini pour le type CRON");
                }
                break;

            default:
                throw new IllegalArgumentException("Type de fréquence non supporté: " + config.getType());
        }
    }

    /**
     * Validation basique d'une expression cron.
     * Vérifie le format standard à 6 ou 7 champs.
     * 
     * @param cronExpression L'expression cron à valider
     * @return true si l'expression semble valide, false sinon
     */
    private boolean isValidCronExpression(String cronExpression) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            return false;
        }

        String[] parts = cronExpression.trim().split("\\s+");
        // Accepte les expressions cron standard (6 champs) ou avec secondes (7 champs)
        return parts.length == 6 || parts.length == 7;
    }

    /**
     * Convertit la configuration active en chaîne de description lisible.
     * 
     * @return La description de la configuration active ou "Aucune configuration active"
     */
    public String getActiveConfigDescription() {
        return getActiveConfig().map(config -> {
            switch (config.getType()) {
                case INTERVAL:
                    return String.format("Exécution toutes les %d minutes", config.getIntervalMinutes());
                case CRON:
                    return String.format("Exécution selon l'expression cron: %s", config.getCronExpression());
                default:
                    return "Configuration de type inconnu";
            }
        }).orElse("Aucune configuration active");
    }
}