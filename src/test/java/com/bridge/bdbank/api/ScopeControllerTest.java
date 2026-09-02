package com.bridge.bdbank.api;

import com.bridge.bdbank.auth.AuthenticationException;
import com.bridge.bdbank.auth.AuthenticationService;
import com.bridge.bdbank.introspection.TableInfo;
import com.bridge.bdbank.scope.ScopeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScopeController.class)
class ScopeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScopeService scopeService;

    @MockBean
    private AuthenticationService authenticationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldGetScopeSuccessfullyWithAuth() throws Exception {
        List<TableInfo> tables = List.of(
            new TableInfo("clients", "bd_bank_test"),
            new TableInfo("comptes", "bd_bank_test")
        );

        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(scopeService.getScopedTables()).thenReturn(tables);

        mockMvc.perform(get("/scope")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldGetScopeHandleUnknownScopedTableException() throws Exception {
        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(scopeService.getScopedTables())
            .thenThrow(new com.bridge.bdbank.scope.UnknownScopedTableException("unknown_table"));

        mockMvc.perform(get("/scope")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void shouldFailGetScopeWithoutAuth() throws Exception {
        mockMvc.perform(get("/scope")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldFailGetScopeWithInvalidToken() throws Exception {
        when(authenticationService.validateToken("invalid-token"))
            .thenThrow(new AuthenticationException("Token invalide"));

        mockMvc.perform(get("/scope")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldUpdateScopeSuccessfully() throws Exception {
        when(authenticationService.validateToken(anyString())).thenReturn(null);

        mockMvc.perform(put("/scope")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Set.of("clients", "comptes"))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldFailUpdateScopeWithoutAuth() throws Exception {
        mockMvc.perform(put("/scope")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Set.of("clients", "comptes"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldFailUpdateScopeWithInvalidToken() throws Exception {
        when(authenticationService.validateToken("invalid-token"))
            .thenThrow(new AuthenticationException("Token invalide"));

        mockMvc.perform(put("/scope")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Set.of("clients", "comptes"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCheckInScopeSuccessfullyWithAuth() throws Exception {
        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(scopeService.isInScope("clients")).thenReturn(true);

        mockMvc.perform(get("/scope/clients")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void shouldFailCheckInScopeWithoutAuth() throws Exception {
        mockMvc.perform(get("/scope/clients")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldFailCheckInScopeWithInvalidToken() throws Exception {
        when(authenticationService.validateToken("invalid-token"))
            .thenThrow(new AuthenticationException("Token invalide"));

        mockMvc.perform(get("/scope/clients")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}