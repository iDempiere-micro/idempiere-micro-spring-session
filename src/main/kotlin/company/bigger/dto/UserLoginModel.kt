package company.bigger.dto

/**
 * A data class to send the login parameters.
 * See [ILogin] for limitations.
 */
data class UserLoginModel(
    override val loginName: String,
    override val password: String,
    override val clientId: Int? = null
) : ILogin