package company.bigger.test.clients.response

data class UserLoginModelResponse(
    val logged: Boolean,
    val token: String?
)