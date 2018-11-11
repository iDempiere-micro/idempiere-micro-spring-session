package company.bigger.service

import company.bigger.dto.UserLoginModel
import company.bigger.dto.UserLoginModelResponse
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.compiere.crm.MUser
import org.idempiere.common.util.Env
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import java.util.Date

@Service
class UserService(
    val loginService: LoginService,
    @Value("\${jwt.secret}") val jwtSecret: String,
    @Value("\${jwt.issuer}")val jwtIssuer: String
) : UserDetailsService {
    override fun loadUserByUsername(username: String?): UserDetails {
        val user = loginService.findByUsername(username).firstOrNull() ?: throw UsernameNotFoundException("Could not find account with username $username!")
        return User(username, user.password, arrayListOf(SimpleGrantedAuthority(ROLE)))
    }

    companion object {
        // user login result by UserName
        private val users = mutableMapOf<String, UserLoginModelResponse>()
        const val ROLE = "USER"
    }

    fun findByToken(token: String?) = users.values.firstOrNull { token != null && it.token == token }

    fun clearCurrentUser() {
        loginService.clearCurrentUser()
    }

    fun setCurrentUser(user: UserLoginModelResponse?): UserLoginModelResponse? {
        loginService.setCurrentUser(user)
        return user
    }

    private fun newToken(user: UserLoginModelResponse): String {
        return Jwts.builder()
                .setIssuedAt(Date())
                .setSubject(user.loginName)
                .setIssuer(jwtIssuer)
                .setExpiration(Date(System.currentTimeMillis() + 10 * 24 * 60 * 60 * 1000)) // 10 days
                .signWith(SignatureAlgorithm.HS256, jwtSecret).compact()
    }

    fun validToken(token: String, user: UserLoginModelResponse?): Boolean {
        val claims = Jwts.parser().setSigningKey(jwtSecret)
                .parseClaimsJws(token).body
        return claims.subject == user?.loginName && claims.issuer == jwtIssuer &&
                Date().before(claims.expiration)
    }

    private fun updateToken(user: UserLoginModelResponse): UserLoginModelResponse {
        val result = user.copy(token = newToken(user))
        users[result.loginName] = result
        return result
    }

    fun login(login: UserLoginModel): UserLoginModelResponse? {
        val user = loginService.login(login)
        if (user.logged) return updateToken(user)
        return user
    }

    fun currentUser(): MUser? {
        return loginService.currentUser()
    }

    fun getUsers(): List<MUser> {
        val user = currentUser()!!
        val ctx = Env.getCtx()
        return MUser.getOfClient(ctx, user.adClientID, null).map { it }
    }
}