package ch.planetebowl.printagent.data.remote

import ch.planetebowl.printagent.domain.repository.SettingsRepository
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

/**
 * L'URL de l'API est configurable par l'utilisateur dans les reglages, alors que Retrofit
 * exige une baseUrl fixe a la construction du client. On construit donc Retrofit avec un
 * host factice ([PLACEHOLDER_BASE_URL]) et cet intercepteur reecrit scheme/host/port/chemin
 * de base a partir des reglages courants juste avant chaque appel — ainsi un changement
 * d'URL dans les reglages est pris en compte immediatement, sans reconstruire Retrofit/OkHttp.
 */
class DynamicBaseUrlInterceptor @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val configuredBaseUrl = runBlocking { settingsRepository.getSettings().apiBaseUrl }
        val configured = configuredBaseUrl.toHttpUrlOrNull()
            ?: throw IOException("URL de l'API non configurée ou invalide : \"$configuredBaseUrl\"")

        val newUrl = original.url.newBuilder()
            .scheme(configured.scheme)
            .host(configured.host)
            .port(configured.port)
            .encodedPath(joinPaths(configured.encodedPath, original.url.encodedPath))
            .build()

        return chain.proceed(original.newBuilder().url(newUrl).build())
    }

    private fun joinPaths(basePath: String, relativePath: String): String {
        val cleanBase = basePath.removeSuffix("/")
        val cleanRelative = relativePath.removePrefix("/")
        return if (cleanRelative.isEmpty()) cleanBase.ifEmpty { "/" } else "$cleanBase/$cleanRelative"
    }

    companion object {
        const val PLACEHOLDER_BASE_URL = "https://printagent.invalid/"
    }
}
