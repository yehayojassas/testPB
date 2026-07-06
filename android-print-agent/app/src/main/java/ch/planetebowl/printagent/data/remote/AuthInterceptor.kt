package ch.planetebowl.printagent.data.remote

import ch.planetebowl.printagent.data.security.SecureTokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Ajoute `Authorization: Bearer {token}` a chaque requete. Le token est lu depuis
 * EncryptedSharedPreferences a chaque appel (jamais mis en cache en clair dans un champ
 * de cette classe) afin qu'un changement de token dans les reglages soit pris en compte
 * immediatement, sans reconstruire le client OkHttp.
 */
class AuthInterceptor @Inject constructor(
    private val secureTokenStore: SecureTokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = secureTokenStore.getToken()
        val request = chain.request().newBuilder()
            .apply {
                if (!token.isNullOrBlank()) {
                    header("Authorization", "Bearer $token")
                }
            }
            .build()
        return chain.proceed(request)
    }
}
