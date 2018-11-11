package org.idempiere.common.db

import java.io.Serializable

/**
 * Adempiere Statement Value Object
 *
 * @author Jorg Janke
 * @version $Id: CStatementVO.java,v 1.2 2006/07/30 00:54:35 jjanke Exp $
 */
class CStatementVO
/**
 * VO Constructor
 *
 * @param resultSetType - ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE,
 * ResultSet.TYPE_SCROLL_SENSITIVE
 * @param resultSetConcurrency - ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE
 */
(resultSetType: Int, resultSetConcurrency: Int) : Serializable {

    /** Type  */
    /**
     * Get ResultSet Type
     *
     * @return rs type
     */
    /**
     * Set ResultSet Type
     *
     * @param resultSetType type
     */
    var resultSetType: Int = 0
    /** Concurrency  */
    /**
     * Get ResultSet Concurrency
     *
     * @return rs concurrency
     */
    /**
     * Set ResultSet Concurrency
     *
     * @param resultSetConcurrency concurrency
     */
    var resultSetConcurrency: Int = 0
    /** SQL Statement  */
    /**
     * Get SQL
     *
     * @return sql
     */
    /**
     * Set SQL. Replace ROWID with TRIM(ROWID) for remote SQL to convert into String as ROWID is not
     * serialized
     *
     * @param sql sql
     */
    var sql: String? = null //	getSql
    //	setSql
    /** Transaction Name *  */
    /** @return transaction name
     */
    /**
     * Set transaction name
     *
     * @param trxName
     */
    var trxName: String? = null

    init {
        this.resultSetType = resultSetType
        this.resultSetConcurrency = resultSetConcurrency
    } //	CStatementVO

    /**
     * VO Constructor
     *
     * @param resultSetType - ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE,
     * ResultSet.TYPE_SCROLL_SENSITIVE
     * @param resultSetConcurrency - ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE
     * @param sql sql
     */
    constructor(resultSetType: Int, resultSetConcurrency: Int, sql: String) : this(resultSetType, resultSetConcurrency) {
        this.sql = sql
    } //	CStatementVO

    /**
     * String representation
     *
     * @return info
     */
    override fun toString(): String {
        val sb = StringBuilder("CStatementVO[")
        sb.append("SQL=" + sql!!)
        if (trxName != null) sb.append(" TrxName=" + trxName!!)
        sb.append("]")
        return sb.toString()
    } //	toString

    companion object {

        /** Serialization Info *  */
        internal const val serialVersionUID = -3393389471515956399L
    }
} //	CStatementVO
