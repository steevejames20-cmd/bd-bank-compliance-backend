package com.bridge.bdbank.introspection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SchemaIntrospectionServiceTest {

    @Autowired
    private SchemaIntrospectionService schemaIntrospectionService;

    @Test
    void devraitListerLesTablesDuSeed() throws Exception {
        List<TableInfo> tables = schemaIntrospectionService.listTables();

        assertThat(tables)
                .extracting(TableInfo::name)
                .contains("clients", "comptes", "transactions");
    }

    @Test
    void devraitListerLesColonnesDeLaTableClients() throws Exception {
        List<ColumnInfo> columns = schemaIntrospectionService.listColumns("clients");

        assertThat(columns)
                .extracting(ColumnInfo::name)
                .contains("id", "nom", "email", "date_naissance", "pays");
    }
}
