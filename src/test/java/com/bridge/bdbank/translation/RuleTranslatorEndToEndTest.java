package com.bridge.bdbank.translation;

import com.bridge.bdbank.dsl.DslParserService;
import com.bridge.bdbank.dsl.ParsedRule;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de bout en bout (J10) : règle DSL en texte -> parsing -> traduction
 * SQL -> exécution sur la vraie base de test locale -> vérification des
 * lignes/groupes en violation. Couvre les 5 cas du traducteur (J8 + J9).
 * <p>
 * Contrairement au reste du projet (Mockito, schéma simulé), ces tests
 * nécessitent MySQL démarré ({@code docker compose up -d}), le seed du J1
 * ET la colonne {@code comptes.decouvert_autorise} ajoutée au J10 (voir
 * db/002_add_decouvert_autorise.sql - à appliquer manuellement si le
 * conteneur existait déjà avant le J10, voir rapport_j10.md).
 * <p>
 * Marqués {@code @Tag("e2e")} pour rester exclus de {@code mvn test} par
 * défaut - à lancer explicitement (voir rapport_j10.md pour la commande).
 * Le compte utilisé pour la connexion est celui configuré par
 * l'application (le compte technique lecture seule du J2), via le
 * {@link DataSource} du contexte Spring - aucune connexion parallèle
 * créée ici.
 */
@Tag("e2e")
@SpringBootTest
@ActiveProfiles("e2e")
class RuleTranslatorEndToEndTest {

    @Autowired
    @Qualifier("bankDataSource")
    private DataSource dataSource;

    @Autowired
    private DslParserService dslParserService;

    @Autowired
    private RuleTranslator ruleTranslator;

    @Test
    void cas1_colonneVsValeur_comptesSoldeNegatif() throws Exception {
        // Anomalie: solde < 0 (comptes avec solde négatif)
        List<Object> violations = executer("comptes.solde < 0", "comptes");

        assertThat(violations).containsExactlyInAnyOrder(2L, 7L);
    }

    @Test
    void cas2_colonneVsColonne_memeTable_soldeSousDecouvertAutorise() throws Exception {
        // Anomalie: solde < decouvert_autorise (comptes sous le découvert autorisé)
        List<Object> violations = executer("comptes.solde < comptes.decouvert_autorise", "comptes");

        assertThat(violations).containsExactlyInAnyOrder(2L, 7L);
    }

    @Test
    void cas3_colonneVsColonne_deuxTables_transactionSousLeDecouvertAutorise() throws Exception {
        // Anomalie: montant < decouvert_autorise (transactions sous le découvert autorisé)
        List<Object> violations = executer(
                "transactions.montant < comptes.decouvert_autorise ON transactions.compte_id == comptes.id",
                "transactions");

        assertThat(violations).containsExactly(3L);
    }

    @Test
    void cas4_agregatVsValeur_sommeTransactionsNegativeParCompte() throws Exception {
        // Anomalie: somme < 0 (comptes avec somme de transactions négative)
        List<Object> groupesEnViolation = executer(
                "SUM(transactions.montant) < 0 GROUP BY compte_id", "transactions");

        assertThat(groupesEnViolation).containsExactlyInAnyOrder(2L, 7L);
    }

    @Test
    void cas5_agregatVsColonne_deuxTables_sommeTransactionsSousDecouvertAutorise() throws Exception {
        // Anomalie: somme < decouvert_autorise (comptes où la somme des transactions est sous le découvert autorisé)
        List<Object> groupesEnViolation = executer(
                "SUM(transactions.montant) < comptes.decouvert_autorise ON transactions.compte_id == comptes.id",
                "transactions");

        assertThat(groupesEnViolation).containsExactly(2L);
    }

    /**
     * Parse, traduit et exécute une règle contre la vraie base, puis
     * retourne la valeur de la première colonne du SELECT généré
     * (identifiant de ligne pour les cas 1-3, clé de groupe pour les cas
     * 4-5) pour chaque résultat - suffisant pour vérifier quelles
     * lignes/groupes sont en violation.
     */
    private List<Object> executer(String dslText, String targetTable) throws Exception {
        ParsedRule rule = dslParserService.parse(dslText);
        TranslatedQuery query = ruleTranslator.translate(rule, targetTable);

        List<Object> results = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query.sql())) {

            List<Object> params = query.params();
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(resultSet.getObject(1));
                }
            }
        }
        return results;
    }
}
