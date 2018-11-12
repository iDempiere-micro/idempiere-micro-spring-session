package company.bigger.dto

import software.hsharp.core.models.NameKeyPair
import java.util.Arrays

data class UserLoginModelResponse(
        override val logged: Boolean = false,
        override val token: String? = null,
        override val loginName: String,
        override val clientId: Int = -1,
        override val userId: Int = -1
) : ILoginResponse {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserLoginModelResponse

        if (logged != other.logged) return false
        if (token != other.token) return false
        if (loginName != other.loginName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = logged.hashCode()
        result = 31 * result + (token?.hashCode() ?: 0)
        result = 31 * result + (loginName.hashCode())
        return result
    }
}