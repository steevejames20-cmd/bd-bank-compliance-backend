package com.bridge.bdbank.api;

import com.bridge.bdbank.auth.AuthenticationException;
import com.bridge.bdbank.auth.AuthenticationService;
import com.bridge.bdbank.introspection.SchemaIntrospectionService;
import com.bridge.bdbank.introspection.TableInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebMvcTest(SchemaController.class)
class SchemaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SchemaController schemaController;

    @MockBean
    private SchemaIntrospectionService schemaIntrospectionService;

    @MockBean
    private AuthenticationService authenticationService;

    @Test
    void shouldListTablesSuccessfullyWithAuth() {
        List<TableInfo> tables = List.of(
            new TableInfo("clients", "bd_bank_test"),
            new TableInfo("comptes", "bd_bank_test"),
            new TableInfo("transactions", "bd_bank_test")
        );

        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(schemaIntrospectionService.listTables()).thenReturn(tables);

        ResponseEntity<List<TableInfo>> response = schemaController.listTables("Bearer valid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(3);
        assertThat(response.getBody().get(0).name()).isEqualTo("clients");
        assertThat(response.getBody().get(1).name()).isEqualTo("comptes");
        assertThat(response.getBody().get(2).name()).isEqualTo("transactions");
    }

    @Test
    void shouldListTablesReturnEmptyListWhenNoTables() {
        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(schemaIntrospectionService.listTables()).thenReturn(List.of());

        ResponseEntity<List<TableInfo>> response = schemaController.listTables("Bearer valid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void shouldFailListTablesWithoutAuth() {
        ResponseEntity<List<TableInfo>> response = schemaController.listTables(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldFailListTablesWithInvalidToken() {
        when(authenticationService.validateToken("invalid-token"))
            .thenThrow(new AuthenticationException("Token invalide"));

        ResponseEntity<List<TableInfo>> response = schemaController.listTables("Bearer invalid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldListColumnsSuccessfullyWithAuth() {
        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(schemaIntrospectionService.listColumns("clients")).thenReturn(List.of());

        ResponseEntity<?> response = schemaController.listColumns("clients", "Bearer valid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldFailListColumnsForNonExistentTable() {
        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(schemaIntrospectionService.listColumns("nonexistent"))
            .thenThrow(new com.bridge.bdbank.introspection.TableNotFoundException("nonexistent"));

        ResponseEntity<?> response = schemaController.listColumns("nonexistent", "Bearer valid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldFailListColumnsWithoutAuth() {
        ResponseEntity<?> response = schemaController.listColumns("clients", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldFailListColumnsWithInvalidToken() {
        when(authenticationService.validateToken("invalid-token"))
            .thenThrow(new AuthenticationException("Token invalide"));

        ResponseEntity<?> response = schemaController.listColumns("clients", "Bearer invalid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}