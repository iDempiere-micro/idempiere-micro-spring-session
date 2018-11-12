package org.idempiere.common.util

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement

internal fun <T> String.asResource(work: (String) -> T): T {
    val content = Ini::class.java.getResource(this).readText()
    return work(content)
}

internal fun <T : Any> String.executeSql(params: Map<Int, Any> = mapOf(), factory: (ResultSet) -> T): List<T> {
    val cnn: Connection = DB.getConnectionRO()
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
