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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebMvcTest(RuleController.class)
class RuleControllerTest {

    @Autowired
    private RuleController ruleController;

    @MockBean
    private RuleRepository ruleRepository;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private RuleValidationService ruleValidationService;

    @Test
    void shouldListRulesSuccessfullyWithAuth() {
        List<Rule> rules = List.of(
            createRule(1L, "age < 18", "clients", RuleSeverity.HIGH),
            createRule(2L, "balance < 0", "comptes", RuleSeverity.CRITICAL)
        );

        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(ruleRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(new PageImpl<>(rules));

        ResponseEntity<Page<RuleResponse>> response = ruleController.listRules(0, 25, "Bearer valid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().getContent().get(0).getId()).isEqualTo(1L);
    }

    @Test
    void shouldListRulesReturnEmptyPageWhenNoRules() {
        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(ruleRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(Page.empty());

        ResponseEntity<Page<RuleResponse>> response = ruleController.listRules(0, 25, "Bearer valid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void shouldFailListRulesWithoutAuth() {
        ResponseEntity<Page<RuleResponse>> response = ruleController.listRules(0, 25, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldGetRuleSuccessfullyWithAuth() {
        Rule rule = createRule(1L, "age < 18", "clients", RuleSeverity.HIGH);

        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(rule));

        ResponseEntity<RuleResponse> response = ruleController.getRule(1L, "Bearer valid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getDslText()).isEqualTo("age < 18");
    }

    @Test
    void shouldFailGetRuleForNonExistentRule() {
        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(ruleRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<RuleResponse> response = ruleController.getRule(999L, "Bearer valid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldFailGetRuleWithoutAuth() {
        ResponseEntity<RuleResponse> response = ruleController.getRule(1L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldCreateRuleSuccessfullyWithAuth() {
        RuleRequest request = RuleRequest.builder()
            .dslText("age < 18")
            .targetTable("clients")
            .severity(RuleSeverity.HIGH)
            .active(true)
            .build();

        Rule savedRule = createRule(1L, "age < 18", "clients", RuleSeverity.HIGH);

        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(ruleRepository.save(any(Rule.class))).thenReturn(savedRule);

        ResponseEntity<RuleResponse> response = ruleController.createRule(request, "Bearer valid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getDslText()).isEqualTo("age < 18");
    }

    @Test
    void shouldFailCreateRuleWithoutAuth() {
        RuleRequest request = RuleRequest.builder()
            .dslText("age < 18")
            .targetTable("clients")
            .severity(RuleSeverity.HIGH)
            .build();

        ResponseEntity<RuleResponse> response = ruleController.createRule(request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldUpdateRuleSuccessfullyWithAuth() {
        RuleRequest request = RuleRequest.builder()
            .dslText("age >= 21")
            .targetTable("clients")
            .severity(RuleSeverity.CRITICAL)
            .active(false)
            .build();

        Rule existingRule = createRule(1L, "age < 18", "clients", RuleSeverity.HIGH);
        Rule updatedRule = createRule(1L, "age >= 21", "clients", RuleSeverity.CRITICAL);
        updatedRule.setActive(false);

        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(existingRule));
        when(ruleRepository.save(any(Rule.class))).thenReturn(updatedRule);

        ResponseEntity<RuleResponse> response = ruleController.updateRule(1L, request, "Bearer valid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getDslText()).isEqualTo("age >= 21");
        assertThat(response.getBody().getSeverity()).isEqualTo(RuleSeverity.CRITICAL);
    }

    @Test
    void shouldFailUpdateRuleForNonExistentRule() {
        RuleRequest request = RuleRequest.builder()
            .dslText("age >= 21")
            .targetTable("clients")
            .severity(RuleSeverity.CRITICAL)
            .build();

        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(ruleRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<RuleResponse> response = ruleController.updateRule(999L, request, "Bearer valid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldFailUpdateRuleWithoutAuth() {
        RuleRequest request = RuleRequest.builder()
            .dslText("age >= 21")
            .targetTable("clients")
            .severity(RuleSeverity.CRITICAL)
            .build();

        ResponseEntity<RuleResponse> response = ruleController.updateRule(1L, request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldDeleteRuleSuccessfullyWithAuth() {
        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(ruleRepository.existsById(1L)).thenReturn(true);

        ResponseEntity<Void> response = ruleController.deleteRule(1L, "Bearer valid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void shouldFailDeleteRuleForNonExistentRule() {
        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(ruleRepository.existsById(999L)).thenReturn(false);

        ResponseEntity<Void> response = ruleController.deleteRule(999L, "Bearer valid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldFailDeleteRuleWithoutAuth() {
        ResponseEntity<Void> response = ruleController.deleteRule(1L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldValidateRuleSuccessfullyWithAuth() {
        RuleRequest request = RuleRequest.builder()
            .dslText("age < 18")
            .targetTable("clients")
            .severity(RuleSeverity.HIGH)
            .build();

        ValidationResult validationResult = ValidationResult.ok();

        when(authenticationService.validateToken(anyString())).thenReturn(null);
        when(ruleValidationService.validate("age < 18", "clients")).thenReturn(validationResult);

        ResponseEntity<ValidationResult> response = ruleController.validateRule(request, "Bearer valid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void shouldFailValidateRuleWithoutAuth() {
        RuleRequest request = RuleRequest.builder()
            .dslText("age < 18")
            .targetTable("clients")
            .severity(RuleSeverity.HIGH)
            .build();

        ResponseEntity<ValidationResult> response = ruleController.validateRule(request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
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