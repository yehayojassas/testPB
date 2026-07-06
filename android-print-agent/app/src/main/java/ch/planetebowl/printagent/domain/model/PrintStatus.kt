package ch.planetebowl.printagent.domain.model

/**
 * Machine a etats de la file locale. Voir le sweep de demarrage
 * (PrinterForegroundService) pour la transition speciale PRINTING -> FAILED_MANUAL_REVIEW
 * appliquee apres un crash.
 */
enum class PrintStatus {
    /** Recu du backend, jamais encore reclame. */
    PENDING,

    /** Claim serveur obtenu, envoi TCP en cours. Persiste AVANT tout octet envoye a
     * l'imprimante pour qu'un crash pendant l'impression soit detectable au redemarrage. */
    PRINTING,

    /** Ticket physiquement envoye (socket flush OK) mais accuse serveur pas encore confirme. */
    PRINTED_PENDING_ACK,

    /** Accuse serveur confirme (ou commande deja completee ailleurs) : etat terminal. */
    COMPLETED,

    /** Echec d'impression, nouvelle tentative planifiee a nextRetryAt. */
    RETRY_WAIT,

    /** Nombre max de tentatives atteint, ou etat serveur incoherent : necessite une
     * intervention humaine, plus aucun retry automatique. */
    FAILED_MANUAL_REVIEW,
}
