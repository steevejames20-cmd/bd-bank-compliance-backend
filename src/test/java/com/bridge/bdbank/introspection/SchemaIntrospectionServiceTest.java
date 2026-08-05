package com.bridge.bdbank.introspection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class SchemaIntrospectionServiceTest {

    @Autowired
    private SchemaIntrospectionService schemaIntrospectionService;

    @Test
    void devraitListerLesTablesDuSeed() {
        List<TableInfo> tables = schemaIntrospectionService.listTables();

        assertThat(tables)
                .extracting(TableInfo::name)
                .contains("clients", "comptes", "transactions");
    }

    @Test
    void devraitListerLesColonnesDeLaTableClients() {
        List<ColumnInfo> columns = schemaIntrospectionService.listColumns("clients");

        assertThat(columns)
                .extracting(ColumnInfo::name)
                .contains("id", "nom", "email", "date_naissance", "pays");
    }

    @Test
    void devraitListerLesColonnesDeLaTableComptes() {
        List<ColumnInfo> columns = schemaIntrospectionService.listColumns("comptes");

        assertThat(columns)
                .extracting(ColumnInfo::name)
                .contains("id", "client_id", "iban", "solde", "devise", "statut");

        // "solde" est un DECIMAL (montant monétaire) : type important à
        // bien détecter, différent des VARCHAR/BIGINT déjà couverts.
        assertThat(columns)
                .filteredOn(c -> c.name().equals("solde"))
                .extracting(ColumnInfo::typeName)
                .containsExactly("DECIMAL");
    }

    @Test
    void devraitListerLesColonnesDeLaTableTransactions() {
        List<ColumnInfo> columns = schemaIntrospectionService.listColumns("transactions");

        assertThat(columns)
                .extracting(ColumnInfo::name)
                .contains("id", "compte_id", "montant", "type", "date_operation");

        // "date_operation" est un DATETIME : encore un type différent
        // (temporel) de ceux déjà couverts par les autres tests.
        assertThat(columns)
                .filteredOn(c -> c.name().equals("date_operation"))
                .extracting(ColumnInfo::typeName)
                .containsExactly("DATETIME");
    }

    @Test
    void devraitEchouerClairementSurUneTableInexistante() {
        assertThatThrownBy(() -> schemaIntrospectionService.listColumns("table_qui_nexiste_pas"))
                .isInstanceOf(TableNotFoundException.class)
                .hasMessageContaining("table_qui_nexiste_pas");
    }
}
