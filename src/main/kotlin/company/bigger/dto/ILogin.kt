package company.bigger.dto

interface ILogin {
    val loginName: String
    val password: String
    val clientId: Int?
    val roleId: Int?
    val orgId: Int?
    val warehouseId: Int?
    val language: String?
}