package company.bigger.util

import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.ResultSet

private fun getConnection(): Connection {
    val ds = HikariDataSource()
    ds.jdbcUrl = Ini.instance.connection
    return ds.connection
}

fun <T : Any> String.executeSql(params: Map<Int, Any> = mapOf(), factory: (ResultSet) -> T): List<T> {
    val cnn: Connection = getConnection()
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
