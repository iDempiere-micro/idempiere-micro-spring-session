package company.bigger.service

import company.bigger.dto.UserLoginModel
import company.bigger.dto.UserLoginModelResponse
import company.bigger.util.Ini
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import kotliquery.HikariCP
import kotliquery.sessionOf
import kotliquery.using
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date

@Service
class UserService(
    val loginService: LoginService,
    @Value("\${jwt.secret}") val jwtSecret: String,
    @Value("\${jwt.issuer}")val jwtIssuer: String
) {
    companion object {
        // user login result by UserName
        private val users = mutableMapOf<String, UserLoginModelResponse>()
        const val ROLE = "USER"
    }

    private fun newToken(user: UserLoginModelResponse): String {
        return Jwts.builder()
                .setIssuedAt(Date())
                .setSubject(user.loginName)
                .setIssuer(jwtIssuer)
                .setExpiration(Date(System.currentTimeMillis() + 10 * 24 * 60 * 60 * 1000)) // 10 days
                .signWith(SignatureAlgorithm.HS256, jwtSecret).compact()
    }

    private fun updateToken(user: UserLoginModelResponse): UserLoginModelResponse {
        val result = user.copy(token = newToken(user))
        users[result.loginName] = result
        return result
    }

    fun login(login: UserLoginModel): UserLoginModelResponse? {
        return using(sessionOf(HikariCP.dataSource())) { session ->
            val user = loginService.login(session, login)
            if (user.logged) updateToken(user) else user
        }
    }
}