package com.bridge.bdbank.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Trace d'un envoi de notification, qu'il ait réussi ou échoué. Conservée
 * pour l'audit : quelle règle a déclenché quel mail, à qui, quand, avec
 * quel contenu exact.
 * <p>
 * ruleName et recipients sont une photo au moment de l'envoi (pas une
 * relation vivante) : si la règle est renommée ou supprimée plus tard,
 * l'historique reste lisible tel qu'il était réellement à l'envoi.
 */
@Entity
@Table(name = "notification_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ruleId;

    @Column(nullable = false, length = 150)
    private String ruleName;

    @Column(nullable = false)
    private Long alertId;

    /**
     * Adresses destinataires au moment de l'envoi, séparées par des virgules.
     */
    @Column(nullable = false, length = 1000)
    private String recipients;

    @Column(nullable = false, length = 250)
    private String subject;

    @Column(nullable = false, length = 4000)
    private String body;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private boolean success;

    /**
     * Message d'erreur si l'envoi a échoué (ex: serveur SMTP injoignable). Null si succès.
     */
    @Column(length = 500)
    private String errorMessage;
}
