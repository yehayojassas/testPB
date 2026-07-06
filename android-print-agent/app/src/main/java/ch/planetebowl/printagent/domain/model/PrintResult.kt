package ch.planetebowl.printagent.domain.model

/**
 * Resultat typo de l'envoi TCP a l'imprimante. Volontairement pas de cas "PAPER_OUT" :
 * en ESC/POS generique unidirectionnel (pas de retour capteur lu), l'app ne peut jamais
 * affirmer avec certitude que le papier manque — seulement que l'ecriture a echoue ou
 * reussi au niveau socket. Voir TcpEscPosPrinterClient pour le detail.
 */
sealed class PrintResult {
    data object Success : PrintResult()
    data class InvalidIpAddress(val rawValue: String) : PrintResult()
    data object NetworkUnavailable : PrintResult()
    data object ConnectionRefusedOrPortClosed : PrintResult()
    data object ConnectTimeout : PrintResult()
    data object WriteTimeout : PrintResult()
    data object ConnectionInterrupted : PrintResult()
    data class DnsResolutionFailed(val host: String) : PrintResult()
    data class UnknownIoError(val detail: String) : PrintResult()

    val isSuccess: Boolean get() = this is Success

    /** Code stable persiste dans lastErrorCode — jamais un message localise volatile. */
    fun errorCode(): String? = when (this) {
        is Success -> null
        is InvalidIpAddress -> "PRINTER_INVALID_IP"
        is NetworkUnavailable -> "PRINTER_NETWORK_UNAVAILABLE"
        is ConnectionRefusedOrPortClosed -> "PRINTER_CONNECTION_REFUSED"
        is ConnectTimeout -> "PRINTER_CONNECT_TIMEOUT"
        is WriteTimeout -> "PRINTER_WRITE_TIMEOUT"
        is ConnectionInterrupted -> "PRINTER_CONNECTION_INTERRUPTED"
        is DnsResolutionFailed -> "PRINTER_DNS_FAILED"
        is UnknownIoError -> "PRINTER_UNKNOWN_ERROR"
    }

    fun userMessage(): String = when (this) {
        is Success -> "Impression envoyée avec succès."
        is InvalidIpAddress -> "Adresse IP imprimante invalide : $rawValue"
        is NetworkUnavailable -> "Réseau Wi-Fi indisponible sur la tablette."
        is ConnectionRefusedOrPortClosed -> "Imprimante injoignable (port fermé ou éteinte)."
        is ConnectTimeout -> "Délai dépassé pour se connecter à l'imprimante."
        is WriteTimeout -> "Délai dépassé pendant l'envoi du ticket."
        is ConnectionInterrupted -> "Connexion à l'imprimante interrompue en cours d'envoi."
        is DnsResolutionFailed -> "Impossible de résoudre l'adresse $host."
        is UnknownIoError -> "Erreur imprimante inattendue."
    }
}
