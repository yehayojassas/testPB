package ch.planetebowl.printagent.domain.usecase

import ch.planetebowl.printagent.domain.model.PrintResult
import ch.planetebowl.printagent.domain.repository.PrinterClient
import ch.planetebowl.printagent.printing.EscPosCommands
import javax.inject.Inject

/**
 * Bouton "Tester la connexion" des reglages : verifie que le port TCP de l'imprimante est
 * joignable sans imprimer de ticket visible. ESC/POS n'offrant pas de ping applicatif, on
 * envoie uniquement la commande d'initialisation (ESC @) — inoffensive, ne fait ni couper
 * le papier ni ouvrir le tiroir, contrairement a TestPrintUseCase.
 */
class TestConnectionUseCase @Inject constructor(
    private val printerClient: PrinterClient,
) {
    suspend operator fun invoke(host: String, port: Int): PrintResult =
        printerClient.send(host, port, EscPosCommands.INIT)
}
