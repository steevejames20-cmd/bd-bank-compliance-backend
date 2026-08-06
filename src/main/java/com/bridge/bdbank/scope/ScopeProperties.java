package com.bridge.bdbank.scope;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Lit le périmètre déclaré dans application.yml (bdbank.scope.tables),
 * lui-même alimenté par la variable d'environnement BDBANK_SCOPE_TABLES
 * (voir .env.example) — une simple liste de noms de tables séparés par
 * des virgules, que Spring convertit automatiquement en List&lt;String&gt;.
 * <p>
 * Volontairement basé sur la config pour l'instant, pas encore persisté en
 * base : la persistance interne de l'outil (semaine 3) permettra de le
 * rendre modifiable à chaud plutôt que figé au démarrage.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bdbank.scope")
public class ScopeProperties {

    private List<String> tables = new ArrayList<>();
}
