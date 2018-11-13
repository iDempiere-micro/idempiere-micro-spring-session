package company.bigger.dto

data class UserLoginModelResponse(
    override val logged: Boolean = false,
    override val token: String? = null,
    override val loginName: String,
    override val clientId: Int = -1,
    override val userId: Int = -1
) : ILoginResponse