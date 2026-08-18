package com.bridge.bdbank.persistence;

/**
 * Statuts possibles pour une alerte de conformité.
 * Une alerte peut être active (anomalie détectée) ou résolue (anomalie corrigée).
 */
public enum AlertStatus {
    /**
     * L'anomalie est actuellement détectée
     */
    ACTIVE,

    /**
     * L'anomalie n'est plus détectée (auto-résolue ou résolue manuellement)
     */
    RESOLVED
}