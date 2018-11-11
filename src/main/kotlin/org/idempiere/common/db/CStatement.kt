package org.idempiere.common.db

import java.sql.SQLException
import java.sql.Statement
import javax.sql.RowSet

/**
 * Interface to wrap and extend Statement
 *
 * @author Low Heng Sin
 */
interface CStatement : Statement {
    /**
     * Get Sql
     *
     * @return sql
     */
    val sql: String

    /**
     * Execute Query
     *
     * @return ResultSet or RowSet
     * @throws SQLException
     * @see java.sql.PreparedStatement.executeQuery
     */
    val rowSet: RowSet

    /**
     * @return boolean
     * @throws SQLException
     */
    @Throws(SQLException::class)
    override fun isClosed(): Boolean

    /** @throws SQLException
     */
    @Throws(SQLException::class)
    fun commit()
} //	CStatement
