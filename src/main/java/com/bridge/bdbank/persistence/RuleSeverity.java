package com.bridge.bdbank.persistence;

/**
 * Niveaux de gravité pour les règles de conformité.
 * Permet de prioriser les alertes générées.
 */
public enum RuleSeverity {
    /**
     * Gravité faible : informationnel, ne bloque pas les processus
     */
    LOW,

    /**
     * Gravité moyenne : attention requise, impact modéré
     */
    MEDIUM,

    /**
     * Gravité haute : action corrective nécessaire, impact important
     */
    HIGH,

    /**
     * Gravité critique : intervention immédiate requise
     */
    CRITICAL
}