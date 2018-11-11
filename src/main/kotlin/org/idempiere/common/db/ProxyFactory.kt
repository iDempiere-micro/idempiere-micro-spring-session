package org.idempiere.common.db

import java.lang.reflect.Proxy

/**
 * Factory class to instantiate dynamic proxy for CStatement, CPreparedStatement and
 * CCallableStatement
 *
 * @author Low Heng Sin
 */
object ProxyFactory {

    /**
     * @param resultSetType
     * @param resultSetConcurrency
     * @param trxName
     * @return CStatement proxy
     */
    fun newCStatement(
            resultSetType: Int, resultSetConcurrency: Int, trxName: String): CStatement {
        return Proxy.newProxyInstance(
                CStatement::class.java!!.getClassLoader(),
                arrayOf(CStatement::class.java),
                StatementProxy(resultSetType, resultSetConcurrency, trxName)) as CStatement
    }

    /**
     * @param resultSetType
     * @param resultSetConcurrency
     * @param sql
     * @param trxName
     * @return CPreparedStatement proxy
     */
    fun newCPreparedStatement(
            resultSetType: Int, resultSetConcurrency: Int, sql: String, trxName: String): CPreparedStatement {
        return Proxy.newProxyInstance(
                CPreparedStatement::class.java!!.getClassLoader(),
                arrayOf(CPreparedStatement::class.java),
                PreparedStatementProxy(resultSetType, resultSetConcurrency, sql, trxName)) as CPreparedStatement
    }

    /**
     * @param resultSetType
     * @param resultSetConcurrency
     * @param sql
     * @param trxName
     * @return CCallableStatement proxy
     */
    fun newCCallableStatement(
            resultSetType: Int, resultSetConcurrency: Int, sql: String, trxName: String?): CCallableStatement {
        return Proxy.newProxyInstance(
                CCallableStatement::class.java!!.getClassLoader(),
                arrayOf(CCallableStatement::class.java),
                CallableStatementProxy(resultSetType, resultSetConcurrency, sql, trxName)) as CCallableStatement
    }

    /**
     * @param info
     * @return CStatement proxy
     */
    fun newCStatement(info: CStatementVO): CStatement {
        return Proxy.newProxyInstance(
                CStatement::class.java!!.getClassLoader(),
                arrayOf(CStatement::class.java),
                StatementProxy(info)) as CStatement
    }

    /**
     * @param info
     * @return CPreparedStatement proxy
     */
    fun newCPreparedStatement(info: CStatementVO): CPreparedStatement {
        return Proxy.newProxyInstance(
                CPreparedStatement::class.java!!.getClassLoader(),
                arrayOf(CPreparedStatement::class.java),
                PreparedStatementProxy(info)) as CPreparedStatement
    }

    /**
     * @param info
     * @return CCallableStatement proxy
     */
    fun newCCallableStatement(info: CStatementVO): CCallableStatement {
        return Proxy.newProxyInstance(
                CCallableStatement::class.java!!.getClassLoader(),
                arrayOf(CCallableStatement::class.java),
                CallableStatementProxy(info)) as CCallableStatement
    }
}
