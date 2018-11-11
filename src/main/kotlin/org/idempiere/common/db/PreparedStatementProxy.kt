package org.idempiere.common.db

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.logging.Level
import javax.sql.RowSet
import javax.sql.rowset.RowSetProvider
import org.idempiere.common.util.DB

/**
 * Dynamic proxy for the CPreparedStatement interface
 *
 * @author Low Heng Sin
 */
class PreparedStatementProxy : StatementProxy {

    override val rowSet: RowSet?
        get() {
            log.trace("local_getRowSet")

            var rowSet: RowSet? = null
            var rs: ResultSet? = null
            val pstmt = p_stmt as PreparedStatement?
            try {
                rs = pstmt!!.executeQuery()
                val crs = RowSetProvider.newFactory().createCachedRowSet()
                crs.populate(rs)
                rowSet = crs
            } catch (ex: Exception) {
                log.error(p_vo!!.toString(), ex)
                throw RuntimeException(ex)
            } finally {
                DB.close(rs)
            }
            return rowSet
        } //	local_getRowSet

    constructor(
            resultSetType: Int, resultSetConcurrency: Int, sql0: String?, trxName: String) {
        if (sql0 == null || sql0.length == 0) throw IllegalArgumentException("sql required")

        p_vo = CStatementVO(
                resultSetType, resultSetConcurrency, DB.database!!.convertStatement(sql0))

        p_vo!!.setTrxName(trxName)

        init()
    } // PreparedStatementProxy

    constructor(vo: CStatementVO) : super(vo) {} //	PreparedStatementProxy

    /** Initialise the prepared statement wrapper object  */
    override fun init() {
        try {
            var conn: Connection? = null
            val trx = if (p_vo!!.getTrxName() == null) null else Trx.get(p_vo!!.getTrxName(), false)
            if (trx != null) {
                conn = trx!!.getConnection()
            } else {
                m_conn = AutoCommitConnectionBroker.connection
                conn = m_conn
            }
            if (conn == null) throw DBException("No Connection")
            p_stmt = conn.prepareStatement(
                    p_vo!!.getSql(), p_vo!!.getResultSetType(), p_vo!!.getResultSetConcurrency())
        } catch (e: Exception) {
            log.log(Level.SEVERE, p_vo!!.getSql(), e)
            throw DBException(e)
        }

    }
}
