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
import java.sql.Timestamp
import java.util.Date

private val log = KotlinLogging.logger {}

data class User(
    val id: Int, // 1
    val isLocked: Boolean, // 42
    val dateAccountLocked: Timestamp?, // 43
    val dateLastLogin: Timestamp?, // 46
    val password: String?, // 11
    val salt: String?, // 41
    val clientId: Int, // 2
    val failedLoginCount: Int?, // 44
    val userName: String // 9
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

    @Value("\${user.locking.max_login_attempt:10}")
    private lateinit var USER_LOCKING_MAX_LOGIN_ATTEMPT: String

    private fun lockOrUnlockUser(session: Session, user: User) {
        val MAX_ACCOUNT_LOCK_MINUTES = USER_LOCKING_MAX_ACCOUNT_LOCK_MINUTES.toInt()
        val MAX_INACTIVE_PERIOD_DAY = USER_LOCKING_MAX_INACTIVE_PERIOD_DAY.toInt()
        val now = Date().time
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


        private fun lockOrUnlockUsers(session: Session, users: List<User>) {
        for (user in users) { lockOrUnlockUser(session, user) }
    }

    fun findByUsername(session: Session, appUser: String?): List<User> {
        log.info("User=$appUser")

        if (appUser == null || appUser.isEmpty()) {
            log.warn("No Apps User")
            return listOf()
        }

        return "/sql/findByUsername.sql".asResource { s ->
            val usersQuery = queryOf(s, appUser, appUser).map { row -> User(
                row.int(1), row.boolean(42), row.sqlTimestampOrNull(43), row.sqlTimestampOrNull(46),
                row.stringOrNull(11), row.stringOrNull(41), row.int(2), row.intOrNull(44),
                row.string(9)
            ) }.asList

            val users = session.run(usersQuery)

            if (users.isEmpty()) {
                log.error("UserPwdError {} {}", appUser, false)
                listOf<Int>()
            }
            lockOrUnlockUsers(session, users)

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

        val failedUsers = users - authenticatedUsers
        failedUsers.forEach {
            if (!it.isLocked) {
                val count = (it.failedLoginCount ?: 0) + 1
                val MAX_LOGIN_ATTEMPT = USER_LOCKING_MAX_LOGIN_ATTEMPT.toInt()
                val reachMaxAttempt = if (MAX_LOGIN_ATTEMPT in 1..count) {
                    log.warn { "Reached the maximum number of setSecurityContext attempts, user account (${it.userName}) is locked" }
                    true
                } else if (MAX_LOGIN_ATTEMPT > 0) {
                    log.warn { "Invalid User ID or Password (${it.userName}) (Login Attempts: $count / $MAX_LOGIN_ATTEMPT" }
                    if (count == MAX_LOGIN_ATTEMPT - 1) {
                        false
                    } else {
                        false
                    }
                } else {
                    false
                }
                "/sql/updateUserWithFailedCount.sql".asResource { s ->
                    session.run(queryOf(s, if (reachMaxAttempt)"Y" else "N", count, if (reachMaxAttempt) Timestamp(Date().time) else null, it.id).asUpdate)
                }
            }
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
        val businessPartnersIds: List<Int?> = "/sql/checkUserAccess.sql".asResource { s ->
            val usersQuery = queryOf(s, user.id).map { row ->
                    row.intOrNull(2) }.asList

            session.run(usersQuery)
        }
        return !businessPartnersIds.none { it != null }
    }
}