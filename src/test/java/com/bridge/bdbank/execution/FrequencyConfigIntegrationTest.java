package com.bridge.bdbank.execution;

import com.bridge.bdbank.persistence.FrequencyConfig;
import com.bridge.bdbank.persistence.FrequencyConfig.FrequencyType;
import com.bridge.bdbank.persistence.FrequencyConfigRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FrequencyConfigIntegrationTest {

    @Autowired
    private FrequencyConfigService frequencyConfigService;

    @Autowired
    private FrequencyConfigRepository frequencyConfigRepository;

    @Test
    void shouldPersistNextCycleAtInDatabase() {
        // 1. Créer une configuration active via le service
        FrequencyConfig config = FrequencyConfig.builder()
            .type(FrequencyType.INTERVAL)
            .intervalMinutes(10)
            .active(true)
            .build();

        FrequencyConfig saved = frequencyConfigService.createConfig(config);
        Long id = saved.getId();

        // 2. Vérifier que nextCycleAt est calculé et présent dans l'objet retourné
        assertThat(saved.getNextCycleAt()).isNotNull();
        LocalDateTime firstNextCycle = saved.getNextCycleAt();

        // 3. Récupérer depuis le repository et vérifier la persistance
        Optional<FrequencyConfig> retrieved = frequencyConfigRepository.findById(id);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getNextCycleAt()).isNotNull();
        // On compare les secondes car Hibernate peut tronquer les nanosecondes selon la BD
        assertThat(retrieved.get().getNextCycleAt().withNano(0))
            .isEqualTo(firstNextCycle.withNano(0));

        // 4. Simuler une exécution
        frequencyConfigService.updateLastExecution();

        // 5. Vérifier que nextCycleAt a été mis à jour en BD
        FrequencyConfig updated = frequencyConfigRepository.findById(id).orElseThrow();
        assertThat(updated.getNextCycleAt()).isAfter(firstNextCycle);
        assertThat(updated.getLastExecutionAt()).isNotNull();
    }

    @Test
    void shouldHandleMultipleConfigsAndPersistence() {
        // 1. Créer une config 1 active
        FrequencyConfig config1 = FrequencyConfig.builder()
            .type(FrequencyType.INTERVAL)
            .intervalMinutes(15)
            .active(true)
            .build();
        frequencyConfigService.createConfig(config1);

        // 2. Créer une config 2 active (devrait désactiver la 1)
        FrequencyConfig config2 = FrequencyConfig.builder()
            .type(FrequencyType.CRON)
            .cronExpression("0 0 * * * *")
            .active(true)
            .build();
        FrequencyConfig saved2 = frequencyConfigService.createConfig(config2);

        // 3. Vérifier l'état en BD
        FrequencyConfig retrieved1 = frequencyConfigRepository.findAll().stream()
            .filter(c -> !c.getId().equals(saved2.getId()))
            .findFirst().orElseThrow();
        
        assertThat(retrieved1.getActive()).isFalse();
        assertThat(retrieved1.getNextCycleAt()).isNull();

        assertThat(saved2.getActive()).isTrue();
        assertThat(saved2.getNextCycleAt()).isNotNull();
    }
}
