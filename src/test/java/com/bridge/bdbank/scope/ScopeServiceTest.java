package com.bridge.bdbank.scope;

import com.bridge.bdbank.introspection.SchemaIntrospectionService;
import com.bridge.bdbank.introspection.TableInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ScopeServiceTest {

    @Autowired
    private ScopeService scopeService;

    @Autowired
    private SchemaIntrospectionService schemaIntrospectionService;

    @Test
    void devraitRetournerLesTablesDuPerimetreConfigure() {
        List<TableInfo> scoped = scopeService.getScopedTables();

        assertThat(scoped)
                .extracting(TableInfo::name)
                .containsExactlyInAnyOrder("clients", "comptes", "transactions");
    }

    @Test
    void devraitConfirmerQuUneTableEstDansOuHorsPerimetre() {
        assertThat(scopeService.isInScope("clients")).isTrue();
        assertThat(scopeService.isInScope("table_hors_perimetre")).isFalse();
    }

    @Test
    void devraitEchouerSiLePerimetreReferenceUneTableInexistante() {
        // Périmètre volontairement invalide, construit à la main pour ne
        // pas dépendre de la vraie config de l'application.
        ScopeProperties badProperties = new ScopeProperties();
        badProperties.setTables(List.of("clients", "table_qui_nexiste_pas"));

        ScopeService badScopeService = new ScopeService(schemaIntrospectionService, badProperties);

        assertThatThrownBy(badScopeService::getScopedTables)
                .isInstanceOf(UnknownScopedTableException.class)
                .hasMessageContaining("table_qui_nexiste_pas");
    }
}
