package com.bridge.bdbank.api;

import com.bridge.bdbank.api.dto.FrequencyConfigRequest;
import com.bridge.bdbank.api.dto.FrequencyConfigResponse;
import com.bridge.bdbank.auth.AuthenticationException;
import com.bridge.bdbank.auth.AuthenticationService;
import com.bridge.bdbank.execution.FrequencyConfigService;
import com.bridge.bdbank.persistence.FrequencyConfig;
import com.bridge.bdbank.persistence.FrequencyConfig.FrequencyType;
import com.bridge.bdbank.persistence.FrequencyConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConfigController.class)
class ConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FrequencyConfigRepository frequencyConfigRepository;

    @MockBean
    private FrequencyConfigService frequencyConfigService;

    @MockBean
    private AuthenticationService authenticationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldGetFrequencyConfigWithoutAuth() throws Exception {
        mockMvc.perform(get("/config/frequency")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(401));
    }

    @Test
    void shouldGetFrequencyConfigWithAuth() throws Exception {
        LocalDateTime nextCycle = LocalDateTime.now().plusMinutes(5);
        FrequencyConfig config = FrequencyConfig.builder()
            .id(1L)
            .type(FrequencyType.INTERVAL)
            .intervalMinutes(5)
            .active(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        when(frequencyConfigRepository.findByActiveTrue()).thenReturn(Optional.of(config));
        when(frequencyConfigService.computeNextCycleAt(config)).thenReturn(nextCycle);

        mockMvc.perform(get("/config/frequency")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interval").value("5m"))
                .andExpect(jsonPath("$.enabled").value("true"))
                .andExpect(jsonPath("$.nextCycleAt").isNotEmpty());
    }

    @Test
    void shouldReturn404WhenNoActiveConfig() throws Exception {
        when(frequencyConfigRepository.findByActiveTrue()).thenReturn(Optional.empty());

        mockMvc.perform(get("/config/frequency")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(404));
    }

    @Test
    void shouldUpdateFrequencyConfigWithInterval() throws Exception {
        FrequencyConfigRequest request = FrequencyConfigRequest.builder()
            .interval("10m")
            .build();

        LocalDateTime nextCycle = LocalDateTime.now().plusMinutes(10);
        FrequencyConfig updatedConfig = FrequencyConfig.builder()
            .id(1L)
            .type(FrequencyType.INTERVAL)
            .intervalMinutes(10)
            .active(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        when(frequencyConfigService.updateConfig(any(FrequencyConfigRequest.class))).thenReturn(updatedConfig);
        when(frequencyConfigService.computeNextCycleAt(updatedConfig)).thenReturn(nextCycle);

        mockMvc.perform(put("/config/frequency")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interval").value("10m"))
                .andExpect(jsonPath("$.enabled").value("true"))
                .andExpect(jsonPath("$.nextCycleAt").isNotEmpty());
    }

    @Test
    void shouldUpdateFrequencyConfigWithCron() throws Exception {
        FrequencyConfigRequest request = FrequencyConfigRequest.builder()
            .cronExpression("0 */5 * * * *")
            .build();

        LocalDateTime nextCycle = LocalDateTime.now().plusMinutes(5);
        FrequencyConfig updatedConfig = FrequencyConfig.builder()
            .id(1L)
            .type(FrequencyType.CRON)
            .cronExpression("0 */5 * * * *")
            .active(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        when(frequencyConfigService.updateConfig(any(FrequencyConfigRequest.class))).thenReturn(updatedConfig);
        when(frequencyConfigService.computeNextCycleAt(updatedConfig)).thenReturn(nextCycle);

        mockMvc.perform(put("/config/frequency")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cronExpression").value("0 */5 * * * *"))
                .andExpect(jsonPath("$.enabled").value("true"))
                .andExpect(jsonPath("$.nextCycleAt").isNotEmpty());
    }

    @Test
    void shouldReturn400ForInvalidConfig() throws Exception {
        FrequencyConfigRequest request = FrequencyConfigRequest.builder()
            .interval("1m") // En dessous du minimum de 3 minutes
            .build();

        when(frequencyConfigService.updateConfig(any(FrequencyConfigRequest.class)))
            .thenThrow(new IllegalArgumentException("L'intervalle minimum est de 3 minutes"));

        mockMvc.perform(put("/config/frequency")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(400));
    }
}