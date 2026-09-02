package com.bridge.bdbank.api;

import com.bridge.bdbank.api.dto.RuleRequest;
import com.bridge.bdbank.api.dto.RuleResponse;
import com.bridge.bdbank.auth.AuthenticationException;
import com.bridge.bdbank.auth.AuthenticationService;
import com.bridge.bdbank.persistence.Rule;
import com.bridge.bdbank.persistence.RuleRepository;
import com.bridge.bdbank.persistence.RuleSeverity;
import com.bridge.bdbank.validation.RuleValidationService;
import com.bridge.bdbank.validation.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RuleController.class)
class RuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RuleRepository ruleRepository;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private RuleValidationService ruleValidationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldListRulesSuccessfullyWithAuth() throws Exception {
        List<Rule> rules = List.of(
            createRule(1L, "age < 18", "clients", RuleSeverity.HIGH),
            createRule(2L, "balance < 0", "comptes", RuleSeverity.CRITICAL)
        );

        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(ruleRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(rules));

        mockMvc.perform(get("/rules?page=0&size=25")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void shouldListRulesReturnEmptyPageWhenNoRules() throws Exception {
        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(ruleRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/rules?page=0&size=25")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void shouldFailListRulesWithoutAuth() throws Exception {
        mockMvc.perform(get("/rules?page=0&size=25")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldGetRuleSuccessfullyWithAuth() throws Exception {
        Rule rule = createRule(1L, "age < 18", "clients", RuleSeverity.HIGH);

        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(rule));

        mockMvc.perform(get("/rules/1")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.dslText").value("age < 18"));
    }

    @Test
    void shouldFailGetRuleForNonExistentRule() throws Exception {
        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(ruleRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/rules/999")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldFailGetRuleWithoutAuth() throws Exception {
        mockMvc.perform(get("/rules/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCreateRuleSuccessfullyWithAuth() throws Exception {
        RuleRequest request = new RuleRequest();
        request.setDslText("age < 18");
        request.setTargetTable("clients");
        request.setSeverity(RuleSeverity.HIGH);
        request.setActive(true);

        Rule savedRule = createRule(1L, "age < 18", "clients", RuleSeverity.HIGH);

        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(ruleRepository.save(any(Rule.class))).thenReturn(savedRule);

        mockMvc.perform(post("/rules")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.dslText").value("age < 18"));
    }

    @Test
    void shouldFailCreateRuleWithoutAuth() throws Exception {
        RuleRequest request = new RuleRequest();
        request.setDslText("age < 18");
        request.setTargetTable("clients");
        request.setSeverity(RuleSeverity.HIGH);
        request.setActive(true);

        mockMvc.perform(post("/rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldUpdateRuleSuccessfullyWithAuth() throws Exception {
        RuleRequest request = new RuleRequest();
        request.setDslText("age < 21");
        request.setTargetTable("clients");
        request.setSeverity(RuleSeverity.MEDIUM);
        request.setActive(true);

        Rule existingRule = createRule(1L, "age < 18", "clients", RuleSeverity.HIGH);
        Rule updatedRule = createRule(1L, "age < 21", "clients", RuleSeverity.MEDIUM);

        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(existingRule));
        when(ruleRepository.save(any(Rule.class))).thenReturn(updatedRule);

        mockMvc.perform(put("/rules/1")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.dslText").value("age < 21"));
    }

    @Test
    void shouldFailUpdateRuleWithoutAuth() throws Exception {
        RuleRequest request = new RuleRequest();
        request.setDslText("age < 21");
        request.setTargetTable("clients");
        request.setSeverity(RuleSeverity.MEDIUM);
        request.setActive(true);

        mockMvc.perform(put("/rules/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldDeleteRuleSuccessfullyWithAuth() throws Exception {
        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(ruleRepository.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/rules/1")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldFailDeleteRuleWithoutAuth() throws Exception {
        mockMvc.perform(delete("/rules/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldValidateRuleSuccessfullyWithAuth() throws Exception {
        RuleRequest request = new RuleRequest();
        request.setDslText("age < 18");
        request.setTargetTable("clients");

        ValidationResult result = ValidationResult.ok();

        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(ruleValidationService.validate(anyString(), anyString())).thenReturn(result);

        mockMvc.perform(post("/rules/validate")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void shouldFailValidateRuleWithoutAuth() throws Exception {
        RuleRequest request = new RuleRequest();
        request.setDslText("age < 18");
        request.setTargetTable("clients");

        mockMvc.perform(post("/rules/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    private Rule createRule(Long id, String dslText, String targetTable, RuleSeverity severity) {
        return Rule.builder()
            .id(id)
            .dslText(dslText)
            .targetTable(targetTable)
            .severity(severity)
            .active(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
}