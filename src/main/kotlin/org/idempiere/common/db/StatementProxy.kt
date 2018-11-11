package org.idempiere.common.db

import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.logging.Level
import javax.sql.RowSet
import javax.sql.rowset.RowSetProvider
import org.idempiere.common.util.DB
import org.slf4j.LoggerFactory

/**
 * Dynamic proxy for the CStatement interface
 *
 * @author Low Heng Sin
 */
open class StatementProxy:InvocationHandler {

protected var m_conn:Connection? = null

private var close = false

/** Logger  */
  @Transient protected var log = LoggerFactory.getLogger(javaClass)
/** Used if local  */
  @Transient protected var p_stmt:Statement? = null
/** Value Object  */
  protected var p_vo:CStatementVO? = null

private val format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) as SimpleDateFormat

/**
 * Execute Query
 *
 * @return ResultSet or RowSet
 * @throws SQLException
 * @see java.sql.PreparedStatement.executeQuery
 */
  protected open val rowSet:RowSet?
get() {
log.trace("getRowSet")
var rowSet:RowSet? = null
var rs:ResultSet? = null
try
{
rs = p_stmt!!.executeQuery(p_vo!!.sql)
val crs = RowSetProvider.newFactory().createCachedRowSet()
crs.populate(rs)
rowSet = crs
}
catch (ex:Exception) {
log.error(p_vo!!.toString(), ex)
throw RuntimeException(ex)
}
finally
{
DB.close(rs)
rs = null
DB.close(rowSet)
rowSet = null
}
return rowSet
} //	local_getRowSet

/**
 * Get Sql
 *
 * @return sql
 */
   val sql:String?
get() {
    return if (p_vo != null) p_vo!!.sql else null
} //	getSql

 constructor(resultSetType:Int, resultSetConcurrency:Int, trxName:String) {
p_vo = CStatementVO(resultSetType, resultSetConcurrency)
p_vo!!.trxName = trxName

init()
}

 constructor(vo:CStatementVO) {
p_vo = vo
init()
}

 // for subclass
  protected constructor() {}

@Throws(Throwable::class)
override fun invoke(obj:Any, method:Method, args:Array<Any?>?):Any? {
val name = method.name
 // handle special case
    if (name == "executeQuery"
|| name == "executeUpdate"
|| name == "execute"
|| name == "addBatch")
{
if (args != null && args!!.size > 0 && args!![0] != null && args!![0] is String)
{
val sql = args!![0] as String
p_vo!!.sql = DB.database!!.convertStatement(sql)
args[0] = p_vo!!.sql
}
}
else if (name == "close" && (args == null || args!!.size == 0))
{
close()
return null
}
else if (name == "getRowSet" && (args == null || args!!.size == 0))
{
return rowSet
}
else if (name == "isClosed" && (args == null || args!!.size == 0))
{
return close
}
else if (name == "finalize" && (args == null || args!!.size == 0))
{
if (p_stmt != null && !close)
{
this.close()
}
return null
}
else if (name == "commit" && (args == null || args!!.size == 0))
{
commit()
return null
}
else if (name == "getSql" && (args == null || args!!.size == 0))
{
return sql
}

var logSql:String? = null
var logOperation:String? = null
if (log.isTraceEnabled && sql != null)
{
if (name == "executeUpdate" || name == "execute")
{
logSql = sql!!.toUpperCase()
if (logSql!!.startsWith("UPDATE "))
{
logSql = logSql!!.substring("UPDATE ".length).trim { it <= ' ' }
    logOperation = "Update"
}
else if (logSql!!.startsWith("INSERT INTO "))
{
logSql = logSql!!.substring("INSERT INTO ".length).trim { it <= ' ' }
    logOperation = "Insert"
}
else if (logSql!!.startsWith("DELETE FROM "))
{
logSql = logSql!!.substring("DELETE FROM ".length).trim { it <= ' ' }
    logOperation = "Delete"
}
if (logOperation != null)
{
val idxspace = logSql!!.indexOf(' ')
if (idxspace > 0) logSql = logSql!!.substring(0, logSql!!.indexOf(' '))
if (log.isTraceEnabled)
log.trace(
        format.format(Date(System.currentTimeMillis()))
        + ","
        + logOperation
        + ","
        + logSql
        + ","
        + (if (p_vo!!.trxName != null) p_vo!!.trxName else "")
        + " (begin)")
}
}
}
val m = p_stmt!!.javaClass.getMethod(name, *method.parameterTypes)
try
{
return m.invoke(p_stmt, *args!!)
}
catch (e:InvocationTargetException) {
throw DB.getSQLException(e)
}
finally
{
if (log.isTraceEnabled && logSql != null && logOperation != null)
{
log.trace(
(format.format(Date(System.currentTimeMillis()))
+ ","
+ logOperation
+ ","
+ logSql
+ ","
+ (if (p_vo!!.trxName != null) p_vo!!.trxName else "")
+ " (end)"))
}
}
}

/** Initialise the statement wrapper object  */
  protected fun init() {
try
{
var conn:Connection? = null
val trx = if (p_vo!!.trxName == null) null else Trx.get(p_vo!!.trxName, false)
if (trx != null)
{
conn = trx!!.getConnection()
}
else
{
m_conn = AutoCommitConnectionBroker.connection
conn = m_conn
}
if (conn == null) throw DBException("No Connection")
p_stmt = conn!!.createStatement(p_vo!!.resultSetType, p_vo!!.resultSetConcurrency)
}
catch (e:SQLException) {
log.error("CStatement", e)
throw DBException(e)
}

}

/**
 * Close
 *
 * @throws SQLException
 * @see java.sql.Statement.close
 */
  @Throws(SQLException::class)
private fun close() {
if (close) return

try
{
DB.close(p_stmt)
}

finally
{
if (m_conn != null)
{
AutoCommitConnectionBroker.releaseConnection(m_conn)
}
m_conn = null
p_stmt = null
close = true
}
} //	close

/**
 * Commit (if local)
 *
 * @throws SQLException
 */
  @Throws(SQLException::class)
private fun commit() {
if (m_conn != null && !m_conn!!.autoCommit)
{
m_conn!!.commit()
}
} //	commit
}
