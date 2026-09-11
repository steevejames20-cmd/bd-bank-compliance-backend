package com.bridge.bdbank.api;

import com.bridge.bdbank.api.dto.TableMappingRequest;
import com.bridge.bdbank.api.dto.TableMappingResponse;
import com.bridge.bdbank.auth.AuthenticationService;
import com.bridge.bdbank.mapping.TableMappingService;
import com.bridge.bdbank.persistence.TableMapping;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TableMappingController.class)
class TableMappingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TableMappingService tableMappingService;

    @MockBean
    private AuthenticationService authenticationService;

    private TableMapping tableMapping;
    private TableMapping columnMapping;

    @BeforeEach
    void setUp() {
        tableMapping = TableMapping.builder()
                .id(1L)
                .originalTableName("clients")
                .logicalTableName("Clients")
                .originalColumnName(null)
                .logicalColumnName(null)
                .description("Renommage de la table clients")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        columnMapping = TableMapping.builder()
                .id(2L)
                .originalTableName("clients")
                .logicalTableName(null)
                .originalColumnName("id_client")
                .logicalColumnName("ID Client")
                .description("Renommage de la colonne id_client")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testListMappings() throws Exception {
        List<TableMapping> mappings = Arrays.asList(tableMapping, columnMapping);
        when(tableMappingService.findAllActive()).thenReturn(mappings);

        mockMvc.perform(get("/api/mappings")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].originalTableName").value("clients"))
                .andExpect(jsonPath("$[0].logicalTableName").value("Clients"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].originalColumnName").value("id_client"))
                .andExpect(jsonPath("$[1].logicalColumnName").value("ID Client"));
    }

    @Test
    void testListMappingsByTable() throws Exception {
        List<TableMapping> tableMappings = Arrays.asList(tableMapping, columnMapping);
        when(tableMappingService.findByTableName("clients")).thenReturn(tableMappings);

        mockMvc.perform(get("/api/mappings/table/clients")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].originalTableName").value("clients"))
                .andExpect(jsonPath("$[1].originalTableName").value("clients"));
    }

    @Test
    void testCreateMapping() throws Exception {
        TableMappingRequest request = TableMappingRequest.builder()
                .originalTableName("comptes")
                .logicalTableName("Comptes")
                .description("Renommage de la table comptes")
                .active(true)
                .build();

        when(tableMappingService.createMapping(any(TableMapping.class))).thenReturn(tableMapping);

        mockMvc.perform(post("/api/mappings")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalTableName").value("clients"))
                .andExpect(jsonPath("$.logicalTableName").value("Clients"));

        verify(tableMappingService).createMapping(any(TableMapping.class));
    }

    @Test
    void testUpdateMapping() throws Exception {
        TableMappingRequest request = TableMappingRequest.builder()
                .logicalTableName("Clients Updated")
                .active(false)
                .build();

        when(tableMappingService.updateMapping(eq(1L), any(TableMapping.class))).thenReturn(tableMapping);

        mockMvc.perform(put("/api/mappings/1")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(tableMappingService).updateMapping(eq(1L), any(TableMapping.class));
    }

    @Test
    void testDeleteMapping() throws Exception {
        doNothing().when(tableMappingService).deleteMapping(1L);

        mockMvc.perform(delete("/api/mappings/1")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNoContent());

        verify(tableMappingService).deleteMapping(1L);
    }

    @Test
    void testToggleMapping() throws Exception {
        tableMapping.setActive(false);
        when(tableMappingService.toggleMapping(1L)).thenReturn(tableMapping);

        mockMvc.perform(patch("/api/mappings/1/toggle")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        verify(tableMappingService).toggleMapping(1L);
    }
}
