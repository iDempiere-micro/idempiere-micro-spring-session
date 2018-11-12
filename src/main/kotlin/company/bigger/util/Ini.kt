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
    companion object {
        private var _instance: Ini? = null
        internal val instance get() = _instance!!
        private fun setInstance(i: Ini) {
            _instance = i
        }
    }

    init { setInstance(this) }

    @Value("\${session.connection}")
    private lateinit var m_connection: String

    val connection get() = m_connection
}

/**
 * Get system configuration property of type boolean
 *
 * @param Name
 * @param defaultValue
 * @return boolean
 */
fun getBooleanValue(s: String, defaultValue: Boolean = false): Boolean {
    if (s.isEmpty()) return defaultValue

    return if ("Y".equals(s, ignoreCase = true))
        true
    else if ("N".equals(s, ignoreCase = true))
        false
    else
        java.lang.Boolean.valueOf(s)
}
