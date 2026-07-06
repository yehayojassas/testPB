package ch.planetebowl.printagent.data.remote

import ch.planetebowl.printagent.data.remote.dto.ClaimResponseDto
import ch.planetebowl.printagent.data.remote.dto.FailedRequestDto
import ch.planetebowl.printagent.data.remote.dto.FailedResponseDto
import ch.planetebowl.printagent.data.remote.dto.JobDto
import ch.planetebowl.printagent.data.remote.dto.PrintedRequestDto
import ch.planetebowl.printagent.data.remote.dto.PrintedResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Interface Retrofit miroir du contrat print-agent (Edge Function Supabase, fixe, deja
 * deployee cote backend). Les endpoints susceptibles de repondre 409 avec un corps
 * exploitable (claim/printed/failed) renvoient un [Response] brut plutot qu'un objet direct :
 * Retrofit leverait sinon une HttpException sur les 4xx et on perdrait le corps JSON
 * (`{"code": "ALREADY_CLAIMED"}`) necessaire pour distinguer les transitions locales.
 */
interface BackendApi {

    @GET("jobs")
    suspend fun getJobs(
        @Query("restaurantId") restaurantId: String,
        @Query("limit") limit: Int = 20,
    ): List<JobDto>

    @POST("jobs/{orderId}/claim")
    suspend fun claimJob(
        @Path("orderId") orderId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
    ): Response<ClaimResponseDto>

    @POST("jobs/{orderId}/printed")
    suspend fun markPrinted(
        @Path("orderId") orderId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: PrintedRequestDto,
    ): Response<PrintedResponseDto>

    @POST("jobs/{orderId}/failed")
    suspend fun markFailed(
        @Path("orderId") orderId: String,
        @Body body: FailedRequestDto,
    ): Response<FailedResponseDto>
}
