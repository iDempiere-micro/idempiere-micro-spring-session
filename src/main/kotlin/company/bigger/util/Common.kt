package company.bigger.util

import java.sql.Connection
import com.zaxxer.hikari.HikariDataSource

internal fun <T> String.asResource(work: (String) -> T): T {
    val content = Ini::class.java.getResource(this).readText()
    return work(content)
}
