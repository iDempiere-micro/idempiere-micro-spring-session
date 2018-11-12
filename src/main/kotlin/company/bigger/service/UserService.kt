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

    fun login(login: UserLoginModel): UserLoginModelResponse? {
        val user = loginService.login(login)
        if (user.logged) return updateToken(user)
        return user
    }
}