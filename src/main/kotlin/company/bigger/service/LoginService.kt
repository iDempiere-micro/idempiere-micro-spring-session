package company.bigger.service

import company.bigger.dto.ILogin
import company.bigger.dto.UserLoginModelResponse
import company.bigger.util.asResource
import company.bigger.util.getBooleanValue
import company.bigger.util.getSHA512Hash
import company.bigger.util.convertHexString
import kotliquery.Session
import kotliquery.queryOf
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.UnsupportedEncodingException
import java.security.NoSuchAlgorithmException
import java.util.Date

private val log = KotlinLogging.logger {}

data class User(
    val id: kotlin.Int, // 1
    val isLocked: kotlin.Boolean, // 42
    val dateAccountLocked: java.util.Date?, // 43
    val dateLastLogin: java.util.Date?, // 46
    val password: kotlin.String?, // 11
    val salt: kotlin.String?, // 41
    val clientId: Int //
)

@Service
class LoginService {
    @Value("\${user.password.hash:N}")
    private lateinit var USER_PASSWORD_HASH: String

    @Value("\${user.locking.max_account_lock_minutes:180}")
    private lateinit var USER_LOCKING_MAX_ACCOUNT_LOCK_MINUTES: String

    @Value("\${user.locking.max_inactive_period_day:180}")
    private lateinit var USER_LOCKING_MAX_INACTIVE_PERIOD_DAY: String

    @Value("\${user.locking.max_password_age_day:365}")
    private lateinit var USER_LOCKING_MAX_PASSWORD_AGE_DAY: String

    fun findByUsername(session: Session, appUser: String?): List<User> {
        log.info("User=$appUser")

        if (appUser == null || appUser.isEmpty()) {
            log.warn("No Apps User")
            return listOf()
        }

        return "/sql/findByUsername.sql".asResource { s ->
            val usersQuery = queryOf(s, appUser, appUser).map { row -> User(
                row.int(1), row.boolean(42), row.sqlDateOrNull(43), row.sqlDateOrNull(46),
                row.stringOrNull(11), row.stringOrNull(41), row.int(2)
            ) }.asList

            val users = session.run(usersQuery)

            if (users.isEmpty()) {
                log.error("UserPwdError {} {}", appUser, false)
                listOf<Int>()
            }

            val MAX_ACCOUNT_LOCK_MINUTES = USER_LOCKING_MAX_ACCOUNT_LOCK_MINUTES.toInt()
            val MAX_INACTIVE_PERIOD_DAY = USER_LOCKING_MAX_INACTIVE_PERIOD_DAY.toInt()
            val now = Date().time
            for (user in users) {
                if (MAX_ACCOUNT_LOCK_MINUTES > 0 && user.isLocked && user.dateAccountLocked != null) {
                    val minutes = (now - user.dateAccountLocked.time) / (1000 * 60)
                    if (minutes > MAX_ACCOUNT_LOCK_MINUTES) {
                        val inactive =
                            if (MAX_INACTIVE_PERIOD_DAY > 0 && user.dateLastLogin != null) {
                                val days = (now - user.dateLastLogin.getTime()) / (1000 * 60 * 60 * 24)
                                days > MAX_INACTIVE_PERIOD_DAY
                            } else { false }

                        if (!inactive) {
                            "/sql/unlockUser.sql".asResource { s2 ->
                                session.run(queryOf(s2, user.id).asUpdate)
                            }
                        }
                    }
                }

                if (MAX_INACTIVE_PERIOD_DAY > 0 && !user.isLocked && user.dateLastLogin != null) {
                    val days = (now - user.dateLastLogin.time) / (1000 * 60 * 60 * 24)
                    if (days > MAX_INACTIVE_PERIOD_DAY) {
                        "/sql/lockUser.sql".asResource { s2 ->
                            session.run(queryOf(s2, user.id).asUpdate)
                        }
                    }
                }
            }

            users
        }
    }

    fun getUsers(session: Session, appUser: String, appPwd: String): Array<User> {
        log.info("User=$appUser")

        if (appUser.isEmpty()) {
            log.warn("No Apps User")
            return arrayOf()
        }
        if (appPwd.isEmpty()) {
            log.warn("No Apps Password")
            return arrayOf()
        }
        val users = findByUsername(session, appUser)
        if (users.isEmpty()) {
            log.error("UserPwdError {}", appUser)
            return arrayOf()
        }

        val hash_password = getBooleanValue(USER_PASSWORD_HASH)
        val MAX_PASSWORD_AGE = USER_LOCKING_MAX_PASSWORD_AGE_DAY.toInt()

        val authenticatedUsers = users.filter {
            when {
                hash_password -> authenticateHash(it, appPwd)
                else -> // password not hashed
                    it.password != null && it.password == appPwd
            } && !it.isLocked
        }
        return authenticatedUsers.toTypedArray()
    }

    private fun authenticateHash(user: User, planText: String): Boolean {
        val hashedText = user.password ?: "0000000000000000"
        val hexSalt = user.salt ?: "0000000000000000"

        return try {
            getSHA512Hash(1000, planText, convertHexString(hexSalt)) == hashedText
        } catch (ignored: NoSuchAlgorithmException) {
            log.warn("Password hashing not supported by JVM")
            false
        } catch (ignored: UnsupportedEncodingException) {
            log.warn("Password hashing not supported by JVM")
            false
        }
    }

    fun login(session: Session, login: ILogin): UserLoginModelResponse {
        val users = getUsers(session, login.loginName, login.password)
        val user = users.firstOrNull { users.count() == 1 || it.clientId == login.clientId }
        if (user != null) {
            return UserLoginModelResponse(checkUserAccess(session, user), null, login.loginName, user.clientId, user.id)
        }
        return UserLoginModelResponse(loginName = login.loginName)
    }

    private fun checkUserAccess(session: Session, user: User): Boolean {
        val c_bpartner_ids = "/sql/checkUserAccess.sql".asResource { s ->
            val usersQuery = queryOf(s, user.id).map { row ->
                    row.int(2) }.asList

            session.run(usersQuery)
        }
        return !c_bpartner_ids.isEmpty()
    }
}