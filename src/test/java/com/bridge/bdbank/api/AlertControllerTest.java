package com.bridge.bdbank.api;

import com.bridge.bdbank.api.dto.AlertResponse;
import com.bridge.bdbank.auth.AuthenticationException;
import com.bridge.bdbank.auth.AuthenticationService;
import com.bridge.bdbank.persistence.Alert;
import com.bridge.bdbank.persistence.AlertRepository;
import com.bridge.bdbank.persistence.AlertStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertController.class)
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlertRepository alertRepository;

    @MockBean
    private AuthenticationService authenticationService;

    @Test
    void shouldListAlertsWithoutAuth() throws Exception {
        List<Alert> alerts = List.of(
            createAlert(1L, 1L, AlertStatus.ACTIVE, "123"),
            createAlert(2L, 1L, AlertStatus.RESOLVED, "456")
        );
        Page<Alert> page = new PageImpl<>(alerts, PageRequest.of(0, 25, Sort.by("detectedAt").descending()), 2);

        when(alertRepository.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/alerts")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(401));
    }

    @Test
    void shouldListAlertsWithAuth() throws Exception {
        List<Alert> alerts = List.of(
            createAlert(1L, 1L, AlertStatus.ACTIVE, "123"),
            createAlert(2L, 1L, AlertStatus.RESOLVED, "456")
        );
        Page<Alert> page = new PageImpl<>(alerts, PageRequest.of(0, 25, Sort.by("detectedAt").descending()), 2);

        when(alertRepository.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/alerts")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void shouldListAlertsWithStatusFilter() throws Exception {
        List<Alert> activeAlerts = List.of(
            createAlert(1L, 1L, AlertStatus.ACTIVE, "123")
        );
        Page<Alert> page = new PageImpl<>(activeAlerts, PageRequest.of(0, 25, Sort.by("detectedAt").descending()), 1);

        when(alertRepository.findByStatus(eq(AlertStatus.ACTIVE), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/alerts?status=ACTIVE")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    @Test
    void shouldGetAlertById() throws Exception {
        Alert alert = createAlert(1L, 1L, AlertStatus.ACTIVE, "123");

        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        mockMvc.perform(get("/alerts/1")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.violatingEntityId").value("123"));
    }

    @Test
    void shouldReturn404ForNonExistentAlert() throws Exception {
        when(alertRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/alerts/999")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(404));
    }

    private Alert createAlert(Long id, Long ruleId, AlertStatus status, String entityId) {
        return Alert.builder()
            .id(id)
            .ruleId(ruleId)
            .status(status)
            .detectedAt(LocalDateTime.now())
            .violatingEntityId(entityId)
            .consecutiveDetections(1)
            .build();
    }
}