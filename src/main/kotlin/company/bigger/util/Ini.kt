package company.bigger.util

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

/**
 * The original Ini class from iDempiere to collect the configuration parameters.
 * It is still used for the connection string etc.
 * Note some local connection variables (like `jwt.issuer`) if used once-only
 * are declared in the place where needed.
 */
@Configuration
open class Ini {
    @Value("\${session.url}")
    internal lateinit var url: String
    @Value("\${session.username}")
    internal lateinit var username: String
    @Value("\${session.password}")
    internal lateinit var password: String
}

/**
 * Get system configuration property of type boolean
 *
 * @param Name
 * @param defaultValue
 * @return boolean
 */
internal fun getBooleanValue(s: String, defaultValue: Boolean = false): Boolean {
    if (s.isEmpty()) return defaultValue

    return if ("Y".equals(s, ignoreCase = true))
        true
    else if ("N".equals(s, ignoreCase = true))
        false
    else
        java.lang.Boolean.valueOf(s)
}
