package company.bigger.util

import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.ResultSet

val ds = HikariDataSource()

fun <T : Any> String.executeSql(params: Map<Int, Any> = mapOf(), factory: (ResultSet) -> T): List<T> {
    if (ds.jdbcUrl != Ini.instance.connection) {
        ds.jdbcUrl = Ini.instance.connection
    }
    val cnn = ds.connection
    val statement = cnn.prepareStatement(this)
    for (param in params) {
        statement.setObject(param.key, param.value)
    }
    val resultSet = statement.executeQuery()
    return resultSet.use {
        generateSequence {
            if (resultSet.next()) factory(resultSet) else null
        }.toList() // must be inside the use() block
    }
}
