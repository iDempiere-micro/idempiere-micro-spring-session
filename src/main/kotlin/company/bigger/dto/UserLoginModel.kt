package company.bigger.dto

data class UserLoginModel(
    override val loginName: String,
    override val password: String,
    override val clientId: Int? = null,
    override val roleId: Int? = null,
    override val orgId: Int? = null,
    override val warehouseId: Int? = null,
    override val language: String? = "en-US"
) : ILogin