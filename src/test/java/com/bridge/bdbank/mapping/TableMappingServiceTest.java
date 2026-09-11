package com.bridge.bdbank.mapping;

import com.bridge.bdbank.persistence.TableMapping;
import com.bridge.bdbank.persistence.TableMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TableMappingServiceTest {

    @Mock
    private TableMappingRepository tableMappingRepository;

    @InjectMocks
    private TableMappingService tableMappingService;

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
                .build();

        columnMapping = TableMapping.builder()
                .id(2L)
                .originalTableName("clients")
                .logicalTableName(null)
                .originalColumnName("id_client")
                .logicalColumnName("ID Client")
                .description("Renommage de la colonne id_client")
                .active(true)
                .build();
    }

    @Test
    void testFindAllActive() {
        List<TableMapping> allMappings = Arrays.asList(tableMapping, columnMapping);
        when(tableMappingRepository.findAll()).thenReturn(allMappings);

        List<TableMapping> result = tableMappingService.findAllActive();

        assertEquals(2, result.size());
        assertTrue(result.contains(tableMapping));
        assertTrue(result.contains(columnMapping));
    }

    @Test
    void testFindByTableName() {
        List<TableMapping> tableMappings = Arrays.asList(tableMapping, columnMapping);
        when(tableMappingRepository.findByOriginalTableNameAndActiveTrue("clients"))
                .thenReturn(tableMappings);

        List<TableMapping> result = tableMappingService.findByTableName("clients");

        assertEquals(2, result.size());
        verify(tableMappingRepository).findByOriginalTableNameAndActiveTrue("clients");
    }

    @Test
    void testGetLogicalTableName_WithMapping() {
        when(tableMappingRepository.findByOriginalTableNameAndOriginalColumnNameIsNullAndActiveTrue("clients"))
                .thenReturn(Optional.of(tableMapping));

        String result = tableMappingService.getLogicalTableName("clients");

        assertEquals("Clients", result);
    }

    @Test
    void testGetLogicalTableName_WithoutMapping() {
        when(tableMappingRepository.findByOriginalTableNameAndOriginalColumnNameIsNullAndActiveTrue("clients"))
                .thenReturn(Optional.empty());

        String result = tableMappingService.getLogicalTableName("clients");

        assertEquals("clients", result);
    }

    @Test
    void testGetLogicalColumnName_WithMapping() {
        when(tableMappingRepository.findByOriginalTableNameAndOriginalColumnNameAndActiveTrue("clients", "id_client"))
                .thenReturn(Optional.of(columnMapping));

        String result = tableMappingService.getLogicalColumnName("clients", "id_client");

        assertEquals("ID Client", result);
    }

    @Test
    void testGetLogicalColumnName_WithoutMapping() {
        when(tableMappingRepository.findByOriginalTableNameAndOriginalColumnNameAndActiveTrue("clients", "id_client"))
                .thenReturn(Optional.empty());

        String result = tableMappingService.getLogicalColumnName("clients", "id_client");

        assertEquals("id_client", result);
    }

    @Test
    void testCreateMapping() {
        TableMapping newMapping = TableMapping.builder()
                .originalTableName("comptes")
                .logicalTableName("Comptes")
                .active(true)
                .build();

        when(tableMappingRepository.save(any(TableMapping.class))).thenReturn(newMapping);

        TableMapping result = tableMappingService.createMapping(newMapping);

        assertNotNull(result);
        verify(tableMappingRepository).save(newMapping);
    }

    @Test
    void testUpdateMapping() {
        TableMapping updatedMapping = TableMapping.builder()
                .logicalTableName("Clients Updated")
                .active(false)
                .build();

        when(tableMappingRepository.findById(1L)).thenReturn(Optional.of(tableMapping));
        when(tableMappingRepository.save(any(TableMapping.class))).thenReturn(tableMapping);

        TableMapping result = tableMappingService.updateMapping(1L, updatedMapping);

        assertNotNull(result);
        assertEquals("Clients Updated", result.getLogicalTableName());
        assertFalse(result.getActive());
        verify(tableMappingRepository).save(tableMapping);
    }

    @Test
    void testUpdateMapping_NotFound() {
        TableMapping updatedMapping = TableMapping.builder()
                .logicalTableName("Clients Updated")
                .build();

        when(tableMappingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            tableMappingService.updateMapping(1L, updatedMapping);
        });
    }

    @Test
    void testDeleteMapping() {
        when(tableMappingRepository.findById(1L)).thenReturn(Optional.of(tableMapping));
        when(tableMappingRepository.save(any(TableMapping.class))).thenReturn(tableMapping);

        tableMappingService.deleteMapping(1L);

        assertFalse(tableMapping.getActive());
        verify(tableMappingRepository).save(tableMapping);
    }

    @Test
    void testDeleteMapping_NotFound() {
        when(tableMappingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            tableMappingService.deleteMapping(1L);
        });
    }

    @Test
    void testToggleMapping_ActiveToInactive() {
        when(tableMappingRepository.findById(1L)).thenReturn(Optional.of(tableMapping));
        when(tableMappingRepository.save(any(TableMapping.class))).thenReturn(tableMapping);

        TableMapping result = tableMappingService.toggleMapping(1L);

        assertFalse(result.getActive());
        verify(tableMappingRepository).save(tableMapping);
    }

    @Test
    void testToggleMapping_InactiveToActive() {
        tableMapping.setActive(false);
        when(tableMappingRepository.findById(1L)).thenReturn(Optional.of(tableMapping));
        when(tableMappingRepository.save(any(TableMapping.class))).thenReturn(tableMapping);

        TableMapping result = tableMappingService.toggleMapping(1L);

        assertTrue(result.getActive());
        verify(tableMappingRepository).save(tableMapping);
    }
}
