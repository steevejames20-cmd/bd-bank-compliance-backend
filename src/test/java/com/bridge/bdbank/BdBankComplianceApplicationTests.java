package com.bridge.bdbank;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test de fumée : si le contexte Spring démarre, la configuration de la
 * datasource (base de test locale MySQL) est valide et joignable.
 *
 * Prérequis : `docker compose up -d` (voir README) pour que la base de
 * test locale soit disponible pendant le build.
 */
@SpringBootTest
class BdBankComplianceApplicationTests {

    @Test
    void contextLoads() {
    }
}
