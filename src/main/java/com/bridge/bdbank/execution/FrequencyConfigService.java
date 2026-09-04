package com.bridge.bdbank.execution;

import com.bridge.bdbank.api.dto.FrequencyConfigRequest;
import com.bridge.bdbank.persistence.FrequencyConfig;
import com.bridge.bdbank.persistence.FrequencyConfigRepository;
import com.bridge.bdbank.persistence.FrequencyConfig.FrequencyType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronExpression;
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
            existing.setNextCycleAt(null);
            frequencyConfigRepository.save(existing);
            log.info("Configuration précédente (id: {}) désactivée", existing.getId());
        });

        // Calculer et persister le prochain cycle avant sauvegarde
        config.setNextCycleAt(computeNextCycleAt(config));

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

        // Recalculer et persister le prochain cycle
        existing.setNextCycleAt(computeNextCycleAt(existing));

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
        // Recalculer le prochain cycle maintenant que la config est active
        config.setNextCycleAt(computeNextCycleAt(config));
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
     * Met à jour la date de dernière exécution pour la configuration active
     * et recalcule la date du prochain cycle.
     */
    @Transactional
    public void updateLastExecution() {
        frequencyConfigRepository.findByActiveTrue().ifPresent(config -> {
            config.setLastExecutionAt(LocalDateTime.now());
            // Recalculer le prochain cycle en fonction de la nouvelle lastExecutionAt
            config.setNextCycleAt(computeNextCycleAt(config));
            frequencyConfigRepository.save(config);
            log.debug("Dernière exécution et prochain cycle mis à jour pour la configuration (id: {}), nextCycleAt: {}",
                config.getId(), config.getNextCycleAt());
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
     * Met à jour la configuration de fréquence à partir d'un DTO.
     * Crée ou met à jour la configuration active.
     * 
     * @param request La requête de configuration
     * @return La configuration mise à jour
     * @throws IllegalArgumentException si la configuration est invalide
     */
    @Transactional
    public FrequencyConfig updateConfig(FrequencyConfigRequest request) {
        // Déterminer le type de fréquence à partir de la requête
        FrequencyType type;
        Integer intervalMinutes = null;
        String cronExpression = null;
        
        if (request.getInterval() != null && !request.getInterval().isEmpty()) {
            // Format: "5m", "1h", "30m"
            type = FrequencyType.INTERVAL;
            intervalMinutes = parseInterval(request.getInterval());
        } else if (request.getCronExpression() != null && !request.getCronExpression().isEmpty()) {
            type = FrequencyType.CRON;
            cronExpression = request.getCronExpression();
        } else {
            throw new IllegalArgumentException("Soit l'intervalle soit l'expression cron doit être fourni");
        }
        
        // Vérifier si une configuration active existe
        Optional<FrequencyConfig> existingActive = frequencyConfigRepository.findByActiveTrue();
        
        if (existingActive.isPresent()) {
            // Mettre à jour la configuration existante
            FrequencyConfig existing = existingActive.get();
            existing.setType(type);
            existing.setIntervalMinutes(intervalMinutes);
            existing.setCronExpression(cronExpression);
            existing.setActive(true);
            // Recalculer le prochain cycle avec la nouvelle configuration
            existing.setNextCycleAt(computeNextCycleAt(existing));
            return frequencyConfigRepository.save(existing);
        } else {
            // Créer une nouvelle configuration
            FrequencyConfig newConfig = FrequencyConfig.builder()
                .type(type)
                .intervalMinutes(intervalMinutes)
                .cronExpression(cronExpression)
                .active(true)
                .build();
            return createConfig(newConfig);
        }
    }
    
    /**
     * Parse une chaîne d'intervalle en minutes.
     * Supporte les formats: "5m", "1h", "30m", "2h30m"
     * 
     * @param interval La chaîne d'intervalle
     * @return Le nombre de minutes
     * @throws IllegalArgumentException si le format est invalide
     */
    private int parseInterval(String interval) {
        interval = interval.toLowerCase().trim();
        int totalMinutes = 0;
        
        // Extraire les heures
        int hIndex = interval.indexOf('h');
        if (hIndex != -1) {
            String hoursStr = interval.substring(0, hIndex);
            try {
                int hours = Integer.parseInt(hoursStr);
                totalMinutes += hours * 60;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Format d'intervalle invalide: " + interval);
            }
            interval = interval.substring(hIndex + 1);
        }
        
        // Extraire les minutes
        int mIndex = interval.indexOf('m');
        if (mIndex != -1) {
            String minutesStr = interval.substring(0, mIndex);
            try {
                int minutes = Integer.parseInt(minutesStr);
                totalMinutes += minutes;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Format d'intervalle invalide: " + interval);
            }
        }
        
        if (totalMinutes < MINIMUM_INTERVAL_MINUTES) {
            throw new IllegalArgumentException(
                String.format("L'intervalle minimum est de %d minutes (valeur fournie: %d)", 
                    MINIMUM_INTERVAL_MINUTES, totalMinutes));
        }
        
        return totalMinutes;
    }

    /**
     * Calcule la date et l'heure du prochain cycle d'exécution pour une configuration donnée.
     *
     * <ul>
     *   <li>Type INTERVAL : lastExecutionAt + intervalMinutes. Si aucune exécution précédente,
     *       retourne now + intervalMinutes.</li>
     *   <li>Type CRON : utilise {@link CronExpression} de Spring pour calculer la prochaine
     *       occurrence à partir de maintenant.</li>
     * </ul>
     *
     * @param config La configuration de fréquence active
     * @return La date du prochain cycle, ou {@code null} si elle ne peut pas être déterminée
     */
    public LocalDateTime computeNextCycleAt(FrequencyConfig config) {
        if (config == null || !Boolean.TRUE.equals(config.getActive())) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();

        switch (config.getType()) {
            case INTERVAL: {
                LocalDateTime base = config.getLastExecutionAt() != null
                    ? config.getLastExecutionAt()
                    : now;
                return base.plusMinutes(config.getIntervalMinutes());
            }
            case CRON: {
                if (config.getCronExpression() == null || config.getCronExpression().isBlank()) {
                    return null;
                }
                try {
                    CronExpression cron = CronExpression.parse(config.getCronExpression());
                    return cron.next(now);
                } catch (IllegalArgumentException e) {
                    log.warn("Impossible de calculer le prochain cycle pour l'expression cron '{}': {}",
                        config.getCronExpression(), e.getMessage());
                    return null;
                }
            }
            default:
                return null;
        }
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