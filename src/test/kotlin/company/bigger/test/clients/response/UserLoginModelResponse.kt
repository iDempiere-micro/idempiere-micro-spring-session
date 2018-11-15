package company.bigger.test.clients.response

internal data class UserLoginModelResponse(
    val logged: Boolean,
    val token: String?,
    val clientId: Int?
)