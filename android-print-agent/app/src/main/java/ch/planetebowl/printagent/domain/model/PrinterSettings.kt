package ch.planetebowl.printagent.domain.model

enum class TicketWidth(val columns: Int) {
    NARROW_42(42),
    WIDE_48(48),
}

/**
 * Reglages non sensibles (DataStore). Le token API est stocke a part, chiffre, via
 * SecureTokenStore — jamais dans cette classe pour ne pas risquer un log accidentel de
 * l'ensemble des reglages (ex: toString() dans un crash report).
 */
data class PrinterSettings(
    val apiBaseUrl: String,
    val restaurantId: String,
    val storeName: String,
    val printerIp: String,
    val printerPort: Int,
    val pollingIntervalSeconds: Int,
    val cutPaperEnabled: Boolean,
    val cashDrawerEnabled: Boolean,
    val ticketWidth: TicketWidth,
    val developerModeEnabled: Boolean,
) {
    companion object {
        const val MIN_POLLING_INTERVAL_SECONDS = 5
        const val MAX_POLLING_INTERVAL_SECONDS = 15
        const val DEFAULT_PRINTER_PORT = 9100

        fun defaults() = PrinterSettings(
            apiBaseUrl = "",
            restaurantId = "",
            storeName = "PLANÈTE BOWL",
            printerIp = "",
            printerPort = DEFAULT_PRINTER_PORT,
            pollingIntervalSeconds = 8,
            cutPaperEnabled = true,
            cashDrawerEnabled = false,
            ticketWidth = TicketWidth.WIDE_48,
            developerModeEnabled = false,
        )
    }
}
