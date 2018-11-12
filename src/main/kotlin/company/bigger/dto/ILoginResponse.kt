package company.bigger.dto

interface ILoginResponse {
    val loginName: String
    val logged: Boolean
    val token: String?
    val clientId: Int
    val userId: Int
}