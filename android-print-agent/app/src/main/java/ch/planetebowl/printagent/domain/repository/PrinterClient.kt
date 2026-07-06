package ch.planetebowl.printagent.domain.repository

import ch.planetebowl.printagent.domain.model.PrintResult

/**
 * Contrat d'envoi bas niveau vers l'imprimante. Le domain ne connait que cette interface ;
 * l'implementation TCP brute (TcpEscPosPrinterClient) vit dans le package printing/ pour
 * garder la dependance java.net.Socket hors de la couche domain.
 */
interface PrinterClient {

    /** Ouvre une connexion, ecrit [ticketBytes], flush, ferme — jamais deux impressions
     * concurrentes (serialise en interne, voir implementation). */
    suspend fun send(host: String, port: Int, ticketBytes: ByteArray): PrintResult
}
