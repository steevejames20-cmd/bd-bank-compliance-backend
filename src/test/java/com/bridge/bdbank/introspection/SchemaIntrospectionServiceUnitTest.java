package com.bridge.bdbank.introspection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test unitaire "pur" : toutes les dépendances JDBC (DataSource,
 * Connection, DatabaseMetaData, ResultSet) sont simulées avec Mockito.
 * Aucune vraie base de données n'est nécessaire pour lancer ce test -
 * contrairement à {@link SchemaIntrospectionServiceTest} (J3/J4), qui est
 * un test d'intégration et nécessite `docker compose up -d`.
 * <p>
 * Utile pour tester rapidement de la pure logique (ici : que "aucune ligne
 * retournée" déclenche bien {@link TableNotFoundException}) sans dépendre
 * d'infrastructure externe.
 */
@ExtendWith(MockitoExtension.class)
class SchemaIntrospectionServiceUnitTest {

    @Mock
    private DataSource dataSource;
    @Mock
    private Connection connection;
    @Mock
    private DatabaseMetaData metaData;
    @Mock
    private ResultSet resultSet;

    @Test
    void devraitLeverTableNotFoundSiAucuneColonneTrouvee() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(connection.getCatalog()).thenReturn("bd_bank_test");
        when(metaData.getColumns(any(), any(), any(), any())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // aucune ligne -> table inconnue

        SchemaIntrospectionService service = new SchemaIntrospectionService(dataSource);

        assertThatThrownBy(() -> service.listColumns("table_bidon"))
                .isInstanceOf(TableNotFoundException.class)
                .hasMessageContaining("table_bidon");
    }
}
