package com.bridge.bdbank.api;

import com.bridge.bdbank.auth.AuthenticationException;
import com.bridge.bdbank.auth.AuthenticationService;
import com.bridge.bdbank.introspection.TableInfo;
import com.bridge.bdbank.scope.ScopeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebMvcTest(ScopeController.class)
class ScopeControllerTest {

    @Autowired
    private ScopeController scopeController;

    @MockBean
    private ScopeService scopeService;

    @MockBean
    private AuthenticationService authenticationService;

    @Test
    void shouldGetScopeSuccessfullyWithAuth() {
        List<TableInfo> tables = List.of(
            new TableInfo("clients", "bd_bank_test"),
            new TableInfo("comptes", "bd_bank_test")
        );

        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(scopeService.getScopedTables()).thenReturn(tables);

        ResponseEntity<List<TableInfo>> response = scopeController.getScope("Bearer valid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).name()).isEqualTo("clients");
        assertThat(response.getBody().get(1).name()).isEqualTo("comptes");
    }

    @Test
    void shouldGetScopeReturnEmptyListWhenNoTables() {
        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(scopeService.getScopedTables()).thenReturn(List.of());

        ResponseEntity<List<TableInfo>> response = scopeController.getScope("Bearer valid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void shouldGetScopeHandleUnknownScopedTableException() {
        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(scopeService.getScopedTables())
            .thenThrow(new com.bridge.bdbank.scope.UnknownScopedTableException("unknown_table"));

        ResponseEntity<List<TableInfo>> response = scopeController.getScope("Bearer valid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void shouldFailGetScopeWithoutAuth() {
        ResponseEntity<List<TableInfo>> response = scopeController.getScope(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldFailGetScopeWithInvalidToken() {
        when(authenticationService.validateToken("invalid-token"))
            .thenThrow(new AuthenticationException("Token invalide"));

        ResponseEntity<List<TableInfo>> response = scopeController.getScope("Bearer invalid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldUpdateScopeReturnNotImplemented() {
        Set<String> tables = Set.of("clients", "comptes");

        when(authenticationService.validateToken(anyString())).thenReturn(null);

        ResponseEntity<Void> response = scopeController.updateScope(tables, "Bearer valid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
    }

    @Test
    void shouldFailUpdateScopeWithoutAuth() {
        Set<String> tables = Set.of("clients");

        ResponseEntity<Void> response = scopeController.updateScope(tables, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldFailUpdateScopeWithInvalidToken() {
        Set<String> tables = Set.of("clients");

        when(authenticationService.validateToken("invalid-token"))
            .thenThrow(new AuthenticationException("Token invalide"));

        ResponseEntity<Void> response = scopeController.updateScope(tables, "Bearer invalid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldCheckTableInScopeSuccessfully() {
        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(scopeService.isInScope("clients")).thenReturn(true);

        ResponseEntity<Boolean> response = scopeController.isInScope("clients", "Bearer valid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isTrue();
    }

    @Test
    void shouldCheckTableNotInScope() {
        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(scopeService.isInScope("nonexistent")).thenReturn(false);

        ResponseEntity<Boolean> response = scopeController.isInScope("nonexistent", "Bearer valid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isFalse();
    }

    @Test
    void shouldFailCheckInScopeWithoutAuth() {
        ResponseEntity<Boolean> response = scopeController.isInScope("clients", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldFailCheckInScopeWithInvalidToken() {
        when(authenticationService.validateToken("invalid-token"))
            .thenThrow(new AuthenticationException("Token invalide"));

        ResponseEntity<Boolean> response = scopeController.isInScope("clients", "Bearer invalid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}