package ch.planetebowl.printagent.data.remote.dto

data class ClaimResponseDto(
    val claimToken: String,
    val expiresAt: String,
)

data class PrintedRequestDto(
    val claimToken: String,
    val printedAt: String,
)

data class PrintedResponseDto(
    val status: String,
)

data class FailedRequestDto(
    val claimToken: String,
    val errorCode: String,
    val errorMessage: String,
)

data class FailedResponseDto(
    val status: String,
)

/** Corps JSON des reponses 409 : `{"code": "ALREADY_CLAIMED"}` etc. */
data class ApiErrorDto(
    val code: String?,
)
