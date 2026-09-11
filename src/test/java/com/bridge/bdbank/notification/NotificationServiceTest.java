package com.bridge.bdbank.notification;

import com.bridge.bdbank.persistence.Alert;
import com.bridge.bdbank.persistence.NotificationHistory;
import com.bridge.bdbank.persistence.NotificationHistoryRepository;
import com.bridge.bdbank.persistence.Rule;
import com.bridge.bdbank.persistence.RuleSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private NotificationHistoryRepository notificationHistoryRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(mailSender, true, "bridge@bdbank.local", notificationHistoryRepository);
    }

    @Test
    void leContenuDuMailDoitDependreDeLaRegleDeclenchee() {
        Rule ruleSoldeNegatif = Rule.builder()
            .id(1L).name("Solde negatif").description("Detecte les comptes en negatif")
            .dslText("solde < 0").targetTable("comptes").severity(RuleSeverity.CRITICAL)
            .notificationEmails(Set.of("risque@bdbank.local")).build();
        Alert alerteCompte = Alert.builder().id(10L).ruleId(1L).violatingEntityId("42").consecutiveDetections(1).build();

        Rule ruleEmailManquant = Rule.builder()
            .id(2L).name("Email client manquant").description("Verifie la presence d'un email")
            .dslText("email IS NULL").targetTable("clients").severity(RuleSeverity.MEDIUM)
            .notificationEmails(Set.of("conformite@bdbank.local")).build();
        Alert alerteClient = Alert.builder().id(20L).ruleId(2L).violatingEntityId("99").consecutiveDetections(3).build();

        notificationService.notifyAlertTriggered(ruleSoldeNegatif, alerteCompte);
        notificationService.notifyAlertTriggered(ruleEmailManquant, alerteClient);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(captor.capture());
        List<SimpleMailMessage> messages = captor.getAllValues();

        assertThat(messages.get(0).getSubject()).contains("Urgent", "Solde negatif");
        assertThat(messages.get(0).getText()).contains("Detecte les comptes en negatif", "comptes", "42");

        assertThat(messages.get(1).getSubject()).contains("Attention", "Email client manquant");
        assertThat(messages.get(1).getText()).contains("Verifie la presence d'un email", "clients", "99");

        // Deux règles différentes ne doivent jamais produire le même mail.
        assertThat(messages.get(0).getSubject()).isNotEqualTo(messages.get(1).getSubject());
        assertThat(messages.get(0).getText()).isNotEqualTo(messages.get(1).getText());
    }

    @Test
    void neDevraitRienEnvoyerSiAucuneAdresseNestConfiguree() {
        Rule rule = Rule.builder().id(3L).name("Sans notification").notificationEmails(Set.of()).build();
        Alert alert = Alert.builder().id(30L).ruleId(3L).violatingEntityId("1").build();

        notificationService.notifyAlertTriggered(rule, alert);

        verifyNoInteractions(mailSender);
        verifyNoInteractions(notificationHistoryRepository);
    }

    @Test
    void devraitPersisterUnHistoriqueApresUnEnvoiReussi() {
        Rule rule = Rule.builder().id(1L).name("Solde negatif").severity(RuleSeverity.CRITICAL)
            .notificationEmails(Set.of("risque@bdbank.local", "conformite@bdbank.local")).build();
        Alert alert = Alert.builder().id(10L).ruleId(1L).violatingEntityId("42").consecutiveDetections(1).build();

        notificationService.notifyAlertTriggered(rule, alert);

        ArgumentCaptor<NotificationHistory> captor = ArgumentCaptor.forClass(NotificationHistory.class);
        verify(notificationHistoryRepository).save(captor.capture());
        NotificationHistory history = captor.getValue();

        assertThat(history.isSuccess()).isTrue();
        assertThat(history.getRuleName()).isEqualTo("Solde negatif");
        assertThat(history.getRecipients()).contains("risque@bdbank.local", "conformite@bdbank.local");
        assertThat(history.getErrorMessage()).isNull();
    }

    @Test
    void devraitPersisterUnHistoriqueEnEchecSiLenvoiEchoue() {
        Rule rule = Rule.builder().id(1L).name("Solde negatif").severity(RuleSeverity.CRITICAL)
            .notificationEmails(Set.of("risque@bdbank.local")).build();
        Alert alert = Alert.builder().id(10L).ruleId(1L).violatingEntityId("42").consecutiveDetections(1).build();
        doThrow(new MailSendException("Serveur SMTP injoignable")).when(mailSender).send(any(SimpleMailMessage.class));

        notificationService.notifyAlertTriggered(rule, alert);

        ArgumentCaptor<NotificationHistory> captor = ArgumentCaptor.forClass(NotificationHistory.class);
        verify(notificationHistoryRepository).save(captor.capture());
        NotificationHistory history = captor.getValue();

        assertThat(history.isSuccess()).isFalse();
        assertThat(history.getErrorMessage()).contains("Serveur SMTP injoignable");
    }
}
