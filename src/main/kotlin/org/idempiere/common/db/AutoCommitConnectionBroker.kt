package org.idempiere.common.db

import java.sql.Connection
import java.sql.SQLException
import org.idempiere.common.util.DB

/**
 * This class managed the sharing of non-transactional connection per thread.
 *
 * @author hengsin
 */
object AutoCommitConnectionBroker {
    private val threadLocalConnection = object : ThreadLocal<ConnectionReference>() {
        override fun initialValue(): ConnectionReference? {
            return null
        }
    }

    /**
     * Retrieve non-transactional connection for current thread. If none have been allocated yet, a
     * new one will be created from the connection pool.
     *
     * @return Connection
     */
    val connection: Connection
        get() {
            var connReference: ConnectionReference? = threadLocalConnection.get()
            try {
                if (connReference != null && !connReference.connection.isClosed) {
                    connReference.referenceCount++
                    return connReference.connection
                }
            } catch (e: SQLException) {
            }

            val connection = DB.createConnection(true, false, Connection.TRANSACTION_READ_COMMITTED)
            connReference = ConnectionReference(connection)
            threadLocalConnection.set(connReference)
            return connection
        }

    /**
     * Release connection. The connection goes back to pool if reference count is zero.
     *
     * @param conn
     */
    fun releaseConnection(conn: Connection) {
        val connReference = threadLocalConnection.get()
        if (connReference != null && connReference.connection === conn) {
            connReference.referenceCount--
            if (connReference.referenceCount <= 0) {
                threadLocalConnection.set(null)
                try {
                    connReference.connection.close()
                } catch (e: SQLException) {
                }

            }
        } else {
            try {
                conn.close()
            } catch (e: SQLException) {
            }

        }
    }

    private class ConnectionReference internal constructor(internal var connection: Connection) {
        internal var referenceCount: Int = 0

        init {
            referenceCount = 1
        }
    }
}
