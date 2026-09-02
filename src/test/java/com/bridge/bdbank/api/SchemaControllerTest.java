package com.bridge.bdbank.api;

import com.bridge.bdbank.auth.AuthenticationException;
import com.bridge.bdbank.auth.AuthenticationService;
import com.bridge.bdbank.introspection.SchemaIntrospectionService;
import com.bridge.bdbank.introspection.TableInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SchemaController.class)
class SchemaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SchemaIntrospectionService schemaIntrospectionService;

    @MockBean
    private AuthenticationService authenticationService;

    @Test
    void shouldListTablesWithAuth() throws Exception {
        List<TableInfo> tables = List.of(
            new TableInfo("clients", "bd_bank_test"),
            new TableInfo("comptes", "bd_bank_test")
        );

        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(schemaIntrospectionService.listTables()).thenReturn(tables);

        mockMvc.perform(get("/schema/tables")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldFailListTablesWithoutAuth() throws Exception {
        mockMvc.perform(get("/schema/tables")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldFailListTablesWithInvalidToken() throws Exception {
        when(authenticationService.validateToken("invalid-token"))
            .thenThrow(new AuthenticationException("Token invalide"));

        mockMvc.perform(get("/schema/tables")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldListColumnsSuccessfullyWithAuth() throws Exception {
        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(schemaIntrospectionService.listColumns("clients")).thenReturn(List.of());

        mockMvc.perform(get("/schema/tables/clients/columns")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void shouldFailListColumnsForNonExistentTable() throws Exception {
        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(schemaIntrospectionService.listColumns("nonexistent"))
            .thenThrow(new com.bridge.bdbank.introspection.TableNotFoundException("nonexistent"));

        mockMvc.perform(get("/schema/tables/nonexistent/columns")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldFailListColumnsWithoutAuth() throws Exception {
        mockMvc.perform(get("/schema/tables/clients/columns")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldFailListColumnsWithInvalidToken() throws Exception {
        when(authenticationService.validateToken("invalid-token"))
            .thenThrow(new AuthenticationException("Token invalide"));

        mockMvc.perform(get("/schema/tables/clients/columns")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}