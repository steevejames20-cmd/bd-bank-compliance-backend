package com.bridge.bdbank.notification;

import com.bridge.bdbank.persistence.Alert;
import com.bridge.bdbank.persistence.Rule;
import com.bridge.bdbank.persistence.RuleSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

/**
 * Envoie un mail aux adresses configurées sur une règle lorsqu'une alerte
 * apparaît (nouvelle anomalie ou anomalie réactivée). Le sujet du mail
 * reflète la gravité de la règle (code couleur côté interface).
 * <p>
 * Volontairement défensif : une erreur SMTP (serveur non configuré,
 * injoignable, etc.) est logguée mais ne doit jamais faire échouer un
 * cycle de vérification des règles.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    /**
     * Libellé affiché dans le sujet du mail pour chaque gravité, aligné sur
     * le code couleur utilisé côté interface (rouge/orange/jaune/gris).
     */
    private static final Map<RuleSeverity, String> SEVERITY_LABELS = new EnumMap<>(RuleSeverity.class);
    static {
        SEVERITY_LABELS.put(RuleSeverity.LOW, "Information");
        SEVERITY_LABELS.put(RuleSeverity.MEDIUM, "Attention");
        SEVERITY_LABELS.put(RuleSeverity.HIGH, "Action requise");
        SEVERITY_LABELS.put(RuleSeverity.CRITICAL, "Urgent");
    }

    private final JavaMailSender mailSender;
    private final boolean notificationsEnabled;
    private final String fromAddress;

    public NotificationService(
            JavaMailSender mailSender,
            @Value("${bdbank.notifications.enabled:true}") boolean notificationsEnabled,
            @Value("${bdbank.notifications.from:bridge-compliance@bdbank.local}") String fromAddress) {
        this.mailSender = mailSender;
        this.notificationsEnabled = notificationsEnabled;
        this.fromAddress = fromAddress;
    }

    /**
     * Notifie les destinataires configurés sur la règle qu'une alerte vient
     * d'apparaître. Ne fait rien si la règle n'a aucune adresse configurée,
     * ou si les notifications sont désactivées globalement.
     */
    public void notifyAlertTriggered(Rule rule, Alert alert) {
        if (!notificationsEnabled) {
            log.debug("Notifications désactivées (bdbank.notifications.enabled=false), rien envoyé pour la règle {}", rule.getId());
            return;
        }
        if (rule.getNotificationEmails() == null || rule.getNotificationEmails().isEmpty()) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(rule.getNotificationEmails().toArray(new String[0]));
        message.setSubject(buildSubject(rule));
        message.setText(buildBody(rule, alert));

        try {
            mailSender.send(message);
            log.info("Notification envoyée pour la règle {} (gravité {}) à {} destinataire(s)",
                rule.getId(), rule.getSeverity(), rule.getNotificationEmails().size());
        } catch (MailException e) {
            // On ne propage jamais : une messagerie en panne ne doit pas
            // empêcher la détection ni la persistance des alertes.
            log.warn("Échec de l'envoi de la notification pour la règle {} : {}", rule.getId(), e.getMessage());
        }
    }

    private String buildSubject(Rule rule) {
        String label = SEVERITY_LABELS.getOrDefault(rule.getSeverity(), rule.getSeverity().name());
        return "[Bridge][" + label + "] " + rule.getName();
    }

    private String buildBody(Rule rule, Alert alert) {
        return "Une anomalie a été détectée par Bridge." + System.lineSeparator() + System.lineSeparator()
            + "Règle : " + rule.getName() + System.lineSeparator()
            + (rule.getDescription() != null && !rule.getDescription().isBlank()
                ? "Description : " + rule.getDescription() + System.lineSeparator() : "")
            + "Gravité : " + rule.getSeverity() + System.lineSeparator()
            + "Table concernée : " + rule.getTargetTable() + System.lineSeparator()
            + "Élément en anomalie : " + alert.getViolatingEntityId() + System.lineSeparator()
            + "Détections consécutives : " + alert.getConsecutiveDetections() + System.lineSeparator()
            + System.lineSeparator()
            + "Ce mail est généré automatiquement, merci de ne pas y répondre.";
    }
}
