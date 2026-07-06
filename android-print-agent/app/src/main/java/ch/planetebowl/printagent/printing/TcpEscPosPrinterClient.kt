package ch.planetebowl.printagent.printing

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import ch.planetebowl.printagent.domain.model.PrintResult
import ch.planetebowl.printagent.domain.repository.PrinterClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.NoRouteToHostException
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation ESC/POS via socket TCP brut (pas de spooler, pas de nom d'imprimante
 * systeme — connexion IP directe, port 9100 par defaut).
 *
 * LIMITE ASSUMEE : ESC/POS generique est unidirectionnel dans cette implementation (on
 * n'attend aucune reponse de l'imprimante). Un [PrintResult.Success] signifie donc
 * uniquement "les octets ont ete ecrits et flushes avec succes sur le socket", jamais
 * "le ticket est physiquement sorti sans bourrage ni manque de papier" — cette
 * distinction n'est pas verifiable sans lire un retour capteur que ce protocole ne fournit
 * pas ici. Le responsable du point de vente doit visuellement confirmer l'impression.
 */
@Singleton
class TcpEscPosPrinterClient @Inject constructor(
    @ApplicationContext private val context: Context,
) : PrinterClient {

    /** Une seule impression a la fois : deux tickets simultanes sur le meme socket
     * corromprait le flux d'octets recu par l'imprimante. */
    private val printMutex = Mutex()

    override suspend fun send(host: String, port: Int, ticketBytes: ByteArray): PrintResult =
        printMutex.withLock {
            if (!isNetworkAvailable()) return@withLock PrintResult.NetworkUnavailable
            if (host.isBlank()) return@withLock PrintResult.InvalidIpAddress(host)

            var socket: Socket? = null
            try {
                // connect() et write()/flush() sont tous deux bloquants : toute la section
                // reseau doit tourner sur Dispatchers.IO, sinon un appelant sur le thread
                // principal (ex. boutons "Tester la connexion"/"Tester l'impression" via
                // viewModelScope) gele l'UI jusqu'a CONNECT_TIMEOUT_MS en cas d'imprimante
                // injoignable.
                withContext(Dispatchers.IO) {
                    // getByName leve UnknownHostException si la resolution echoue,
                    // deja geree par le catch ci-dessous — pas besoin d'un cas nul separe.
                    val address = java.net.InetAddress.getByName(host)
                    socket = Socket().also { it.connect(InetSocketAddress(address, port), CONNECT_TIMEOUT_MS) }

                    withTimeout(WRITE_TIMEOUT_MS) {
                        socket!!.getOutputStream().apply {
                            write(ticketBytes)
                            flush()
                        }
                    }
                }
                PrintResult.Success
            } catch (e: UnknownHostException) {
                PrintResult.DnsResolutionFailed(host)
            } catch (e: SocketTimeoutException) {
                PrintResult.ConnectTimeout
            } catch (e: TimeoutCancellationException) {
                PrintResult.WriteTimeout
            } catch (e: ConnectException) {
                PrintResult.ConnectionRefusedOrPortClosed
            } catch (e: NoRouteToHostException) {
                PrintResult.NetworkUnavailable
            } catch (e: IOException) {
                PrintResult.ConnectionInterrupted
            } catch (e: Exception) {
                PrintResult.UnknownIoError(e.message ?: e::class.java.simpleName)
            } finally {
                try {
                    socket?.close()
                } catch (e: IOException) {
                    // Fermeture best-effort : une erreur ici ne change pas le resultat deja
                    // determine ci-dessus, mais ne doit surtout pas faire planter l'appelant.
                }
            }
        }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true // Impossible de verifier : on laisse la tentative de connexion trancher.
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    companion object {
        const val CONNECT_TIMEOUT_MS = 5_000
        const val WRITE_TIMEOUT_MS = 8_000L
    }
}
