package company.bigger.util

import java.sql.Connection
import com.zaxxer.hikari.HikariDataSource

internal fun <T> String.asResource(work: (String) -> T): T {
    val content = Ini::class.java.getResource(this).readText()
    return work(content)
}

fun getConnectionRO(): Connection {
    val ds = HikariDataSource()
    ds.jdbcUrl = "jdbc:mysql://localhost:3306/simpsons"
    ds.username = "bart"
    ds.password = "51mp50n"
    return ds.connection
}
