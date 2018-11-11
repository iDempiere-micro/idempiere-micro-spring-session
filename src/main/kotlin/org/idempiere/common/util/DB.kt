package org.idempiere.common.util

import org.idempiere.common.db.CConnection
import java.math.BigDecimal
import java.sql.CallableStatement
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.SQLWarning
import java.sql.Statement
import java.sql.Timestamp
import java.text.MessageFormat
import java.util.ArrayList
import java.util.Properties
import java.util.logging.Level
import javax.sql.RowSet
import org.idempiere.common.db.Database
import org.idempiere.common.db.ProxyFactory
import org.slf4j.LoggerFactory

/**
 * General Database Interface
 *
 * @author Jorg Janke
 * @version $Id: DB.java,v 1.8 2006/10/09 00:22:29 jjanke Exp $ ---
 * @author Ashley Ramdass (Posterita)
 *  * Modifications: removed static references to database connection and instead always get a
 * new connection from database pool manager which manages all connections set rw/ro
 * properties for the connection accordingly.
 * @author Teo Sarca, SC ARHIPAC SERVICE SRL
 *  * BF [ 1647864 ] WAN: delete record error
 *  * FR [ 1884435 ] Add more DB.getSQLValue helper methods
 *  * FR [ 1904460 ] DB.executeUpdate should handle Boolean params
 *  * BF [ 1962568 ] DB.executeUpdate should handle null params
 *  * FR [ 1984268 ] DB.executeUpdateEx should throw DBException
 *  * FR [ 1986583 ] Add DB.executeUpdateEx(String, Object[], String)
 *  * BF [ 2030233 ] Remove duplicate code from DB class
 *  * FR [ 2107062 ] Add more DB.getKeyNamePairs methods
 *  * FR [ 2448461 ] Introduce DB.getSQLValue*Ex methods
 *  * FR [ 2781053 ] Introduce DB.getValueNamePairs
 *  * FR [ 2818480 ] Introduce DB.createT_Selection helper method
 * https://sourceforge.net/tracker/?func=detail&aid=2818480&group_id=176962&atid=879335
 * @author Teo Sarca, teo.sarca@gmail.com
 *  * BF [ 2873324 ] DB.TO_NUMBER should be a static method
 * https://sourceforge.net/tracker/?func=detail&aid=2873324&group_id=176962&atid=879332
 *  * FR [ 2873891 ] DB.getKeyNamePairs should use trxName
 * https://sourceforge.net/tracker/?func=detail&aid=2873891&group_id=176962&atid=879335
 * @author Paul Bowden, phib BF 2900767 Zoom to child tab - inefficient queries
 * @see https://sourceforge.net/tracker/?func=detail&aid=2900767&group_id=176962&atid=879332
 */
 object DB {
/** Database Version as date Compared with AD_System  */
   var DB_VERSION = "2017-10-31"
/** Product Name  */
   val NAME = "iDempiere\u00AE"

/** Connection Descriptor  */
  private var s_cc: CConnection? = null
/** Logger  */
  private val log = LoggerFactory.getLogger(javaClass)

private val s_ccLock = Any()

/** SQL Statement Separator "; "  */
   val SQLSTATEMENT_SEPARATOR = "; "

/** @return true, if connected to database
 */
   val isConnected: Boolean
get() = isConnected(true)

/** @return Connection (r/w)
 */
   val connectionRW: Connection?
get() = getConnectionRW(true)

/**
 * Return everytime a new r/w no AutoCommit, Serializable connection. To be used to ID
 *
 * @return Connection (r/w)
 */
   val connectionID: Connection?
get() = createConnection(false, false, Connection.TRANSACTION_READ_COMMITTED) //  getConnectionID

/**
 * Return read committed, read/only from pool.
 *
 * @return Connection (r/o)
 */
  //  see below
 val connectionRO: Connection?
get() = createConnection(true, true, Connection.TRANSACTION_READ_COMMITTED) // 	getConnectionRO

/**
 * Get Database Driver. Access to database specific functionality.
 *
 * @return Adempiere Database Driver
 */
   val database: AdempiereDatabase?
get() {
if (s_cc != null) return s_cc!!.database
log.error("No Database Connection")
return null
} //  getDatabase

/**
 * Do we have an Oracle DB ?
 *
 * @return true if connected to Oracle
 */
   val isOracle: Boolean
get() {
if (s_cc != null) return s_cc!!.isOracle
log.error("No Database Connection")
return false
} // 	isOracle

 // begin vpj-cd e-evolution 02/07/2005 PostgreSQL
  /**
 * Do we have a Postgre DB ?
 *
 * @return true if connected to PostgreSQL
 */
   val isPostgreSQL: Boolean
get() {
if (s_cc != null) return s_cc!!.isPostgreSQL
log.error("No Database")
return false
} // 	isPostgreSQL
 // begin vpj-cd e-evolution 02/07/2005 PostgreSQL

  /**
 * Get Database Info
 *
 * @return info
 */
   val databaseInfo: String
get() {
    return if (s_cc != null) s_cc!!.dbInfo else "No Database"
} // 	getDatabaseInfo

/**
 * Is this a remote client connection.
 *
 *
 * Deprecated, always return false.
 *
 * @return true if client and RMI or Objects on Server
 */
   val isRemoteObjects: Boolean
@Deprecated("")
get() = false // 	isRemoteObjects

/**
 * Is this a remote client connection
 *
 *
 * Deprecated, always return false.
 *
 * @return true if client and RMI or Process on Server
 */
   val isRemoteProcess: Boolean
@Deprecated("")
get() = false // 	isRemoteProcess

/** Quote  */
  private val QUOTE = '\''

private var m_isUUIDVerified = false
private var m_isUUIDSupported = false
/**
 * *
 *
 * @return true if current db have working generate_uuid function. generate_uuid doesn't work on
 * 64 bit postgresql on windows yet.
 */
   val isGenerateUUIDSupported: Boolean
get() {
if (!m_isUUIDVerified) {
var uuidTest: String? = null
try {
uuidTest = getSQLValueStringEx(null, "SELECT Generate_UUID() FROM Dual")
} catch (e: Exception) {}

m_isUUIDSupported = uuidTest != null && uuidTest!!.trim { it <= ' ' }.length == 36
m_isUUIDVerified = true
}
return m_isUUIDSupported
}

/**
 * ************************************************************************ Check need for post
 * Upgrade
 *
 * @param ctx context
 * @return true if post upgrade ran - false if there was no need DAP public static boolean
 * afterMigration (Properties ctx) { // UPDATE AD_System SET IsJustMigrated='Y' MSystem system
 * = MSystem.get(ctx); if (!system.isJustMigrated()) return false;
 *
 * // Role update log.info("Role"); String sql = "SELECT * FROM AD_Role"; PreparedStatement
 * pstmt = null; ResultSet rs = null; try { pstmt = DB.prepareStatement (sql, null); rs =
 * pstmt.executeQuery (); while (rs.next ()) { MRole role = new MRole (ctx, rs, null);
 * role.updateAccessRecords(); } } catch (Exception e) { log.log(Level.SEVERE, "(1)", e); }
 * finally { close(rs); close(pstmt); rs= null; pstmt = null; } // Release Specif stuff &
 * Print Format try { Class clazz = Class.forName("org.idempiere.MigrateData");
 * clazz.newInstance(); } catch (Exception e) { log.log (Level.SEVERE, "Data", e); }
 *
 * // Language check log.info("Language"); MLanguage.maintain(ctx);
 *
 * // Sequence check log.info("Sequence"); ProcessInfo processInfo = new
 * ProcessInfo("Sequence Check", 0);
 * processInfo.setClassName("org.idempiere.process.SequenceCheck");
 * processInfo.setParameter(new ProcessInfoParameter[0]); ProcessUtil.startJavaProcess(ctx,
 * processInfo, null);
 *
 * // Costing Setup log.info("Costing"); MAcctSchema[] ass =
 * MAcctSchema.getClientAcctSchema(ctx, 0); for (int i = 0; i < ass.length; i++) {
 * ass[i].checkCosting(); ass[i].saveEx(); }
 *
 * // Reset Flag system.setIsJustMigrated(false); return system.save(); } // afterMigration
 */

  /**
 * ************************************************************************ Set connection
 *
 * @param cc connection
 */
  @Synchronized fun setDBTarget(cc: CConnection?) {
if (cc == null) throw IllegalArgumentException("Connection is NULL")

if (s_cc != null && s_cc!!.equals(cc)) return

DB.closeTarget()
 //
    synchronized(s_ccLock) {
s_cc = cc
}

s_cc!!.setDataSource()

      //log.isTraceEnabled
if (log.isTraceEnabled) log.trace(s_cc.toString() + " - DS=" + s_cc!!.isDataSource)
 // 	Trace.printStack();
  } //  setDBTarget

/**
 * Connect to database and initialise all connections.
 *
 * @return True if success, false otherwise
 */
   fun connect(): Boolean {
 // direct connection
    var success = false
try {
val connRW = connectionRW
if (connRW != null) {
s_cc!!.readInfo(connRW)
connRW!!.close()
}

val connRO = connectionRO
if (connRO != null) {
connRO!!.close()
}

val connID = connectionID
if (connID != null) {
connID!!.close()
}
success = connRW != null && connRO != null && connID != null
} catch (e: Exception) {
 // logging here could cause infinite loop
      // log.log(Level.SEVERE, "Could not connect to DB", e);
      System.err.println("Could not connect to DB - " + e.localizedMessage)
e.printStackTrace()
success = false
}

return success
}

/**
 * Is there a connection to the database ?
 *
 * @param createNew If true, try to connect it not already connected
 * @return true, if connected to database
 */
   fun isConnected(createNew: Boolean): Boolean {
 // bug [1637432]
    if (s_cc == null) {
println("s_cc == null")
return false
}

 // direct connection
    var success = false
var eb = CLogErrorBuffer.get(false)
if (eb != null && eb!!.isIssueError())
eb!!.setIssueError(false)
else
eb = null // 	don't reset
try {
val conn = getConnectionRW(createNew) //  try to get a connection
if (conn != null) {
conn!!.close()
success = true
} else
success = false
} catch (e: Exception) {
println("E1")
e.printStackTrace()
success = false
}

if (eb != null) eb!!.setIssueError(true)
return success
} //  isConnected

/**
 * Return (pooled) r/w AutoCommit, Serializable connection. For Transaction control use
 * Trx.getConnection()
 *
 * @param createNew If true, try to create new connection if no existing connection
 * @return Connection (r/w)
 */
   fun getConnectionRW(createNew: Boolean): Connection? {
return createConnection(true, false, Connection.TRANSACTION_READ_COMMITTED)
} //  getConnectionRW

/**
 * Create new Connection. The connection must be closed explicitly by the application
 *
 * @param autoCommit auto commit
 * @param trxLevel - Connection.TRANSACTION_READ_UNCOMMITTED,
 * Connection.TRANSACTION_READ_COMMITTED, Connection.TRANSACTION_REPEATABLE_READ, or
 * Connection.TRANSACTION_READ_COMMITTED.
 * @return Connection connection
 */
   fun createConnection(autoCommit: Boolean, trxLevel: Int): Connection? {
val conn = s_cc!!.getConnection(autoCommit, trxLevel)
if (CLogMgt.isLevelFinest()) { /**
 * try { log.finest(s_cc.getConnectionURL() + ", UserID=" + s_cc.getDbUid() + ", AutoCommit="
 * + conn.getAutoCommit() + " (" + autoCommit + ")" + ", TrxIso=" +
 * conn.getTransactionIsolation() + "( " + trxLevel + ")"); } catch (Exception e) { }
 */
    }

 // hengsin: failed to set autocommit can lead to severe lock up of the system
    try {
if (conn != null && conn!!.getAutoCommit() != autoCommit) {
throw IllegalStateException(
        "Failed to set the requested auto commit mode on connection. [autoCommit=" +
        autoCommit +
        "]")
}
} catch (e: SQLException) {}

return conn
} // 	createConnection

/**
 * Create new Connection. The connection must be closed explicitly by the application
 *
 * @param autoCommit auto commit
 * @param trxLevel - Connection.TRANSACTION_READ_UNCOMMITTED,
 * Connection.TRANSACTION_READ_COMMITTED, Connection.TRANSACTION_REPEATABLE_READ, or
 * Connection.TRANSACTION_READ_COMMITTED.
 * @return Connection connection
 */
   fun createConnection(autoCommit: Boolean, readOnly: Boolean, trxLevel: Int): Connection {
val conn = s_cc!!.getConnection(autoCommit, trxLevel) ?: throw IllegalStateException("DB.getConnectionRO - @NoDBConnection@")

 // hengsin: this could be problematic as it can be reuse for readwrite activites after return to
    // pool
    /*
    if (conn != null)
    {
        try
        {
            conn.setReadOnly(readOnly);
        }
        catch (SQLException ex)
        {
            conn = null;
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }*/

    // hengsin: failed to set autocommit can lead to severe lock up of the system
    try {
if (conn!!.getAutoCommit() != autoCommit) {
throw IllegalStateException(
("Failed to set the requested auto commit mode on connection. [autocommit=" +
autoCommit +
"]"))
}
} catch (e: SQLException) {}

return conn
} //  createConnection

/**
 * Get Database Driver. Access to database specific functionality.
 *
 * @param URL JDBC connection url
 * @return Adempiere Database Driver
 */
   fun getDatabase(URL: String): AdempiereDatabase {
return Database.getDatabaseFromURL(URL)
} //  getDatabase

/**
 * ************************************************************************ Check database Version
 * with Code version
 *
 * @param ctx context
 * @return true if Database version (date) is the same
 */
   fun isDatabaseOK(ctx: Properties): Boolean {
 //    Check Version
    var version = "?"
val sql = "SELECT Version FROM AD_System"
var pstmt: PreparedStatement? = null
var rs: ResultSet? = null
try {
pstmt = prepareStatement(sql, null)
rs = pstmt!!.executeQuery()
if (rs!!.next()) version = rs!!.getString(1)
} catch (e: SQLException) {
log.error(
"Problem with AD_System Table - Run system.sql script - " + e.toString())
return false
} finally
{
close(rs)
close(pstmt)
rs = null
pstmt = null
}
if (log.isInfoEnabled) log.info("DB_Version=$version")
 //  Identical DB version
    return if (DB_VERSION == version) true else false
} //  isDatabaseOK

/**
 * ************************************************************************ Check Build Version of
 * Database against running client
 *
 * @param ctx context
 * @return true if Database version (date) is the same
 */
   fun isBuildOK(ctx: Properties): Boolean {
 //    Check Build
    val buildClient = "5.1.0.v20180116-0927" // TODO DAP Adempiere.getVersion();
var buildDatabase = ""
var failOnBuild = false
val sql = "SELECT LastBuildInfo, IsFailOnBuildDiffer FROM AD_System"
var pstmt: PreparedStatement? = null
var rs: ResultSet? = null
try {
pstmt = prepareStatement(sql, null)
rs = pstmt!!.executeQuery()
if (rs!!.next()) {
buildDatabase = rs!!.getString(1)
failOnBuild = rs!!.getString(2) == "Y"
}
} catch (e: SQLException) {
log.error(
"Problem with AD_System Table - Run system.sql script - " + e.toString())
return false
} finally
{
close(rs)
close(pstmt)
rs = null
pstmt = null
}
if (log.isInfoEnabled) {
log.info("Build DB=$buildDatabase")
log.info("Build Cl=$buildClient")
}
 //  Identical DB version
    if (buildClient == buildDatabase) return true

val AD_Message = "BuildVersionError"
val title = AD_Message // DAP TODO org.idempiere.Adempiere.getName() + " " +  Msg.getMsg(ctx,
 // AD_Message, true);
    // The program assumes build version {0}, but database has build Version {1}.
    var msg = AD_Message // TODO DAP Msg.getMsg(ctx, AD_Message);   //  complete message
msg = MessageFormat.format(msg, *arrayOf<Any>(buildClient, buildDatabase))
if (!failOnBuild) {
log.warn(msg)
return true
}

log.error(msg)
return false
} //  isDatabaseOK

/** ************************************************************************ Close Target  */
   fun closeTarget() {

var closed = false

 //  CConnection
    if (s_cc != null) {
closed = true
s_cc!!.setDataSource(null)
}
s_cc = null
if (closed) log.trace("closed")
} // 	closeTarget

/**
 * ************************************************************************ Prepare Forward Read
 * Only Call
 *
 * @param SQL sql
 * @return Callable Statement
 */
   fun prepareCall(sql: String): CallableStatement {
return prepareCall(sql, ResultSet.CONCUR_UPDATABLE, null)
}

/**
 * ************************************************************************ Prepare Call
 *
 * @param SQL sql
 * @param readOnly
 * @param trxName
 * @return Callable Statement
 */
   fun prepareCall(
       SQL: String?,
       resultSetConcurrency: Int,
       trxName: String?
   ): CallableStatement {
if (SQL == null || SQL!!.length == 0)
throw IllegalArgumentException("Required parameter missing - " + SQL!!)
return ProxyFactory.newCCallableStatement(
ResultSet.TYPE_FORWARD_ONLY, resultSetConcurrency, SQL, trxName)
} // 	prepareCall

/**
 * ************************************************************************ Prepare Statement
 *
 * @param sql
 * @return Prepared Statement
 */
  @Deprecated("")
 fun prepareStatement(sql: String): CPreparedStatement {
var concurrency = ResultSet.CONCUR_READ_ONLY
val upper = sql.toUpperCase()
if (upper.startsWith("UPDATE ") || upper.startsWith("DELETE "))
concurrency = ResultSet.CONCUR_UPDATABLE
return prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, concurrency, null)
} // 	prepareStatement

/**
 * Prepare Statement
 *
 * @param sql
 * @param trxName transaction
 * @return Prepared Statement
 */
   fun prepareStatement(sql: String, trxName: String?): CPreparedStatement {
var concurrency = ResultSet.CONCUR_READ_ONLY
val upper = sql.toUpperCase()
if (upper.startsWith("UPDATE ") || upper.startsWith("DELETE "))
concurrency = ResultSet.CONCUR_UPDATABLE
return prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, concurrency, trxName)
} // 	prepareStatement

/**
 * Prepare Statement.
 *
 * @param sql sql statement
 * @param resultSetType - ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE,
 * ResultSet.TYPE_SCROLL_SENSITIVE
 * @param resultSetConcurrency - ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE
 * @param trxName transaction name
 * @return Prepared Statement r/o or r/w depending on concur
 */
  @JvmOverloads fun prepareStatement(
      sql: String?,
      resultSetType: Int,
      resultSetConcurrency: Int,
      trxName: String? = null
  ): CPreparedStatement {
if (sql == null || sql!!.length == 0) throw IllegalArgumentException("No SQL")
 //
    return ProxyFactory.newCPreparedStatement(resultSetType, resultSetConcurrency, sql, trxName)
} // 	prepareStatement

/**
 * Create Statement.
 *
 * @param resultSetType - ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE,
 * ResultSet.TYPE_SCROLL_SENSITIVE
 * @param resultSetConcurrency - ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE
 * @param trxName transaction name
 * @return Statement - either r/w ir r/o depending on concur
 */
  @JvmOverloads fun createStatement(
      resultSetType: Int = ResultSet.TYPE_FORWARD_ONLY,
      resultSetConcurrency: Int = ResultSet.CONCUR_READ_ONLY,
      trxName: String? = null
  ): Statement {
return ProxyFactory.newCStatement(resultSetType, resultSetConcurrency, trxName)
} // 	createStatement

/**
 * Set parameters for given statement
 *
 * @param stmt statements
 * @param params parameters array; if null or empty array, no parameters are set
 */
  @Throws(SQLException::class)
 fun setParameters(stmt: PreparedStatement?, params: Array<Any>?) {
if (params == null || params!!.size == 0) {
return
}
 //
    for (i in params!!.indices) {
setParameter(stmt, i + 1, params!![i])
}
}

/**
 * Set parameters for given statement
 *
 * @param stmt statements
 * @param params parameters list; if null or empty list, no parameters are set
 */
  @Throws(SQLException::class)
 fun setParameters(stmt: PreparedStatement?, params: List<*>?) {
if (params == null || params!!.size == 0) {
return
}
for (i in params!!.indices) {
setParameter(stmt, i + 1, params!![i])
}
}

/**
 * Set PreparedStatement's parameter. Similar with calling `pstmt.setObject(index, param)
` *
 *
 * @param pstmt
 * @param index
 * @param param
 * @throws SQLException
 */
  @Throws(SQLException::class)
 fun setParameter(pstmt: PreparedStatement?, index: Int, param: Any?) {
if (param == null)
pstmt!!.setObject(index, null)
else if (param is String)
pstmt!!.setString(index, param as String?)
else if (param is Int)
pstmt!!.setInt(index, (param as Int).toInt())
else if (param is BigDecimal)
pstmt!!.setBigDecimal(index, param as BigDecimal?)
else if (param is Timestamp)
pstmt!!.setTimestamp(index, param as Timestamp?)
else if (param is Boolean)
pstmt!!.setString(index, if ((param as Boolean).booleanValue()) "Y" else "N")
else if (param is ByteArray)
pstmt!!.setBytes(index, param as ByteArray?)
else
throw DBException("Unknown parameter type $index - $param")
}

/**
 * Execute Update. saves "DBExecuteError" in Log
 *
 * @param sql sql
 * @param trxName optional transaction name
 * @param timeOut optional timeout parameter
 * @return number of rows updated or -1 if error
 */
  @JvmOverloads fun executeUpdate(sql: String, trxName: String, timeOut: Int = 0): Int {
return executeUpdate(sql, null, false, trxName, timeOut)
} // 	executeUpdate

/**
 * Execute Update. saves "DBExecuteError" in Log
 *
 * @param sql sql
 * @param ignoreError if true, no execution error is reported
 * @return number of rows updated or -1 if error
 */
  @Deprecated("")
 fun executeUpdate(sql: String, ignoreError: Boolean): Int {
return executeUpdate(sql, null, ignoreError, null)
} // 	executeUpdate

/**
 * Execute Update. saves "DBExecuteError" in Log
 *
 * @param sql sql
 * @param ignoreError if true, no execution error is reported
 * @param trxName transaction
 * @param timeOut optional timeOut parameter
 * @return number of rows updated or -1 if error
 */
  @JvmOverloads fun executeUpdate(sql: String, ignoreError: Boolean, trxName: String, timeOut: Int = 0): Int {
return executeUpdate(sql, null, ignoreError, trxName, timeOut)
}

/**
 * Execute Update. saves "DBExecuteError" in Log
 *
 * @param sql sql
 * @param param int param
 * @param trxName transaction
 * @param timeOut optional timeOut parameter
 * @return number of rows updated or -1 if error
 */
  @JvmOverloads fun executeUpdate(sql: String, param: Int, trxName: String, timeOut: Int = 0): Int {
return executeUpdate(sql, arrayOf<Any>(param), false, trxName, timeOut)
} // 	executeUpdate

/**
 * Execute Update. saves "DBExecuteError" in Log
 *
 * @param sql sql
 * @param param int parameter
 * @param ignoreError if true, no execution error is reported
 * @param trxName transaction
 * @param timeOut optional timeOut parameter
 * @return number of rows updated or -1 if error
 */
  @JvmOverloads fun executeUpdate(
      sql: String,
      param: Int,
      ignoreError: Boolean,
      trxName: String,
      timeOut: Int = 0
  ): Int {
return executeUpdate(sql, arrayOf<Any>(param), ignoreError, trxName, timeOut)
} // 	executeUpdate

/**
 * Execute Update. saves "DBExecuteError" in Log
 *
 * @param sql sql
 * @param params array of parameters
 * @param ignoreError if true, no execution error is reported
 * @param trxName optional transaction name
 * @param timeOut optional timeOut parameter
 * @return number of rows updated or -1 if error
 */
  @JvmOverloads fun executeUpdate(
      sql: String?,
      params: Array<Any>? = null,
      ignoreError: Boolean = false,
      trxName: String? = null,
      timeOut: Int = 0
  ): Int {
if (sql == null || sql!!.length == 0)
throw IllegalArgumentException("Required parameter missing - " + sql!!)
verifyTrx(trxName, sql)
 //
    var no = -1
var cs = ProxyFactory.newCPreparedStatement(
ResultSet.TYPE_FORWARD_ONLY,
ResultSet.CONCUR_UPDATABLE,
sql,
trxName) // 	converted in call

try {
setParameters(cs, params)
 // set timeout
      if (timeOut > 0) {
cs!!.setQueryTimeout(timeOut)
}
no = cs!!.executeUpdate()
 // 	No Transaction - Commit
      if (trxName == null) {
cs!!.commit() // 	Local commit
}
} catch (e: Exception) {
e = getSQLException(e)
if (ignoreError)
log.log(Level.SEVERE, cs!!.getSql() + " [" + trxName + "] - " + e!!.message)
else {
log.log(Level.SEVERE, cs!!.getSql() + " [" + trxName + "]", e)
val msg = DBException.getDefaultDBExceptionMessage(e)
log.saveError(if (msg != null) msg else "DBExecuteError", e!!)
}
 // 	throw new DBException(e);
    } finally
{
 //  Always close cursor
      close(cs)
cs = null
}
return no
} // 	executeUpdate

/**
 * Execute Update and throw exception.
 *
 * @param sql
 * @param params statement parameters
 * @param trxName transaction
 * @param timeOut optional timeOut parameter
 * @return number of rows updated
 * @throws SQLException
 */
  @Throws(DBException::class)
@JvmOverloads fun executeUpdateEx(sql: String?, params: Array<Any>?, trxName: String?, timeOut: Int = 0): Int {
if (sql == null || sql!!.length == 0)
throw IllegalArgumentException("Required parameter missing - " + sql!!)
 //
    verifyTrx(trxName, sql)
var no = -1
var cs = ProxyFactory.newCPreparedStatement(
ResultSet.TYPE_FORWARD_ONLY,
ResultSet.CONCUR_UPDATABLE,
sql,
trxName) // 	converted in call

try {
setParameters(cs, params)
if (timeOut > 0) {
run { cs!!.setQueryTimeout(timeOut) }
}
no = cs!!.executeUpdate()
 // 	No Transaction - Commit
      if (trxName == null) {
cs!!.commit() // 	Local commit
}
} catch (e: Exception) {
throw DBException(e)
} finally
{
close(cs)
cs = null
}
return no
}

/**
 * Execute multiple Update statements. saves (last) "DBExecuteError" in Log
 *
 * @param sql multiple sql statements separated by "; " SQLSTATEMENT_SEPARATOR
 * @param ignoreError if true, no execution error is reported
 * @param trxName optional transaction name
 * @return number of rows updated or -1 if error
 */
   fun executeUpdateMultiple(sql: String?, ignoreError: Boolean, trxName: String): Int {
if (sql == null || sql!!.length == 0)
throw IllegalArgumentException("Required parameter missing - " + sql!!)
val index = sql!!.indexOf(SQLSTATEMENT_SEPARATOR)
if (index == -1) return executeUpdate(sql, null, ignoreError, trxName)
var no = 0
 //
    val statements = sql!!.split((SQLSTATEMENT_SEPARATOR).toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
for (i in statements.indices) {
if (log.isLoggable(Level.FINE)) log.fine(statements[i])
no += executeUpdate(statements[i], null, ignoreError, trxName)
}

return no
} // 	executeUpdareMultiple

/**
 * Execute Update and throw exception.
 *
 * @see {@link .executeUpdateEx
 */
  @Throws(DBException::class)
@JvmOverloads fun executeUpdateEx(sql: String, trxName: String, timeOut: Int = 0): Int {
return executeUpdateEx(sql, null, trxName, timeOut)
} // 	executeUpdateEx

/**
 * Commit - commit on RW connection. Is not required as RW connection is AutoCommit (exception:
 * with transaction)
 *
 * @param throwException if true, re-throws exception
 * @param trxName transaction name
 * @return true if not needed or success
 * @throws SQLException
 */
  @Throws(SQLException::class, IllegalStateException::class)
 fun commit(throwException: Boolean, trxName: String?): Boolean {
 // Not on transaction scope, Connection are thus auto commit
    if (trxName == null) {
return true
}

try {
val trx = Trx.get(trxName, false)
if (trx != null) return trx!!.commit(true)

    return if (throwException) {
        throw IllegalStateException("Could not load transation with identifier: " + trxName!!)
    } else {
        false
    }
} catch (e: SQLException) {
log.log(Level.SEVERE, "[$trxName]", e)
if (throwException) throw e
return false
}
} // 	commit

/**
 * Rollback - rollback on RW connection. Is has no effect as RW connection is AutoCommit
 * (exception: with transaction)
 *
 * @param throwException if true, re-throws exception
 * @param trxName transaction name
 * @return true if not needed or success
 * @throws SQLException
 */
  @Throws(SQLException::class)
 fun rollback(throwException: Boolean, trxName: String?): Boolean {
try {
var conn: Connection? = null
val trx = if (trxName == null) null else Trx.get(trxName, true)
if (trx != null)
return trx!!.rollback(true)
else
conn = DB.connectionRW
if (conn != null && !conn!!.autoCommit) conn!!.rollback()
} catch (e: SQLException) {
log.log(Level.SEVERE, "[$trxName]", e)
if (throwException) throw e
return false
}

return true
} // 	commit

/**
 * Get Row Set. When a Rowset is closed, it also closes the underlying connection. If the created
 * RowSet is transfered by RMI, closing it makes no difference
 *
 * @param sql sql
 * @param local local RowSet (own connection)
 * @return row set or null
 */
   fun getRowSet(sql: String): RowSet {
 // Bugfix Gunther Hoppe, 02.09.2005, vpj-cd e-evolution
    val info = CStatementVO(
RowSet.TYPE_SCROLL_INSENSITIVE,
RowSet.CONCUR_READ_ONLY,
DB.database!!.convertStatement(sql))
val stmt = ProxyFactory.newCPreparedStatement(info)
val retValue = stmt.getRowSet()
close(stmt)
return retValue
} // 	getRowSet

/**
 * Get int Value from sql
 *
 * @param trxName trx
 * @param sql sql
 * @param params array of parameters
 * @return first value or -1 if not found
 * @throws DBException if there is any SQLException
 */
  @Throws(DBException::class)
 fun getSQLValueEx(trxName: String, sql: String, vararg params: Any): Int {
var retValue = -1
var pstmt: PreparedStatement? = null
var rs: ResultSet? = null
try {
pstmt = prepareStatement(sql, trxName)
setParameters(pstmt, params)
rs = pstmt!!.executeQuery()
if (rs!!.next())
retValue = rs!!.getInt(1)
else if (log.isLoggable(Level.FINE)) log.fine("No Value $sql")
} catch (e: SQLException) {
throw DBException(e, sql)
} finally
{
close(rs, pstmt)
rs = null
pstmt = null
}
return retValue
}

/**
 * convenient method to close result set and statement
 *
 * @param rs result set
 * @param st statement
 * @see .close
 * @see .close
 */
   fun close(rs: ResultSet?, st: Statement?) {
close(rs)
close(st)
}

/**
 * Get String Value from sql
 *
 * @param trxName trx
 * @param sql sql
 * @param params collection of parameters
 * @return first value or -1
 * @throws DBException if there is any SQLException
 */
   fun getSQLValueEx(trxName: String, sql: String, params: List<Any>): Int {
return getSQLValueEx(trxName, sql, *params.toTypedArray())
}

/**
 * Get int Value from sql
 *
 * @param trxName trx
 * @param sql sql
 * @param params array of parameters
 * @return first value or -1 if not found or error
 */
  @JvmOverloads fun getSQLValue(trxName: String, sql: String, vararg params: Any = arrayOf()): Int {
var retValue = -1
try {
retValue = getSQLValueEx(trxName, sql, *params)
} catch (e: Exception) {
log.log(Level.SEVERE, sql, getSQLException(e))
}

return retValue
}

/**
 * Get int Value from sql
 *
 * @param trxName trx
 * @param sql sql
 * @param params collection of parameters
 * @return first value or null
 */
   fun getSQLValue(trxName: String, sql: String, params: List<Any>): Int {
return getSQLValue(trxName, sql, *params.toTypedArray())
}

/**
 * Get String Value from sql
 *
 * @param trxName trx
 * @param sql sql
 * @param params array of parameters
 * @return first value or null
 * @throws DBException if there is any SQLException
 */
   fun getSQLValueStringEx(trxName: String?, sql: String, vararg params: Any): String? {
var retValue: String? = null
var pstmt: PreparedStatement? = null
var rs: ResultSet? = null
try {
pstmt = prepareStatement(sql, trxName)
setParameters(pstmt, params)
rs = pstmt!!.executeQuery()
if (rs!!.next())
retValue = rs!!.getString(1)
else if (log.isLoggable(Level.FINE)) log.fine("No Value $sql")
} catch (e: SQLException) {
throw DBException(e, sql)
} finally
{
close(rs, pstmt)
rs = null
pstmt = null
}
return retValue
}

/**
 * Get String Value from sql
 *
 * @param trxName trx
 * @param sql sql
 * @param params collection of parameters
 * @return first value or null
 * @throws DBException if there is any SQLException
 */
   fun getSQLValueStringEx(trxName: String, sql: String, params: List<Any>): String? {
return getSQLValueStringEx(trxName, sql, *params.toTypedArray())
}

/**
 * Get String Value from sql
 *
 * @param trxName trx
 * @param sql sql
 * @param params array of parameters
 * @return first value or null
 */
   fun getSQLValueString(trxName: String, sql: String, vararg params: Any): String? {
var retValue: String? = null
try {
retValue = getSQLValueStringEx(trxName, sql, *params)
} catch (e: Exception) {
log.log(Level.SEVERE, sql, getSQLException(e))
}

return retValue
}

/**
 * Get String Value from sql
 *
 * @param trxName trx
 * @param sql sql
 * @param params collection of parameters
 * @return first value or null
 */
   fun getSQLValueString(trxName: String, sql: String, params: List<Any>): String? {
return getSQLValueString(trxName, sql, *params.toTypedArray())
}

/**
 * Get BigDecimal Value from sql
 *
 * @param trxName trx
 * @param sql sql
 * @param params array of parameters
 * @return first value or null if not found
 * @throws DBException if there is any SQLException
 */
  @Throws(DBException::class)
 fun getSQLValueBDEx(trxName: String, sql: String, vararg params: Any): BigDecimal? {
var retValue: BigDecimal? = null
var pstmt: PreparedStatement? = null
var rs: ResultSet? = null
try {
pstmt = prepareStatement(sql, trxName)
setParameters(pstmt, params)
rs = pstmt!!.executeQuery()
if (rs!!.next())
retValue = rs!!.getBigDecimal(1)
else if (log.isLoggable(Level.FINE)) log.fine("No Value $sql")
} catch (e: SQLException) {
 // log.log(Level.SEVERE, sql, getSQLException(e));
      throw DBException(e, sql)
} finally
{
close(rs, pstmt)
rs = null
pstmt = null
}
return retValue
}

/**
 * Get BigDecimal Value from sql
 *
 * @param trxName trx
 * @param sql sql
 * @param params collection of parameters
 * @return first value or null if not found
 * @throws DBException if there is any SQLException
 */
  @Throws(DBException::class)
 fun getSQLValueBDEx(trxName: String, sql: String, params: List<Any>): BigDecimal? {
return getSQLValueBDEx(trxName, sql, *params.toTypedArray())
}

/**
 * Get BigDecimal Value from sql
 *
 * @param trxName trx
 * @param sql sql
 * @param params array of parameters
 * @return first value or null
 */
   fun getSQLValueBD(trxName: String, sql: String, vararg params: Any): BigDecimal? {
try {
return getSQLValueBDEx(trxName, sql, *params)
} catch (e: Exception) {
log.log(Level.SEVERE, sql, getSQLException(e))
}

return null
}

/**
 * Get BigDecimal Value from sql
 *
 * @param trxName trx
 * @param sql sql
 * @param params collection of parameters
 * @return first value or null
 */
   fun getSQLValueBD(trxName: String, sql: String, params: List<Any>): BigDecimal? {
return getSQLValueBD(trxName, sql, *params.toTypedArray())
}

/**
 * Get Timestamp Value from sql
 *
 * @param trxName trx
 * @param sql sql
 * @param params array of parameters
 * @return first value or null
 * @throws DBException if there is any SQLException
 */
   fun getSQLValueTSEx(trxName: String, sql: String, vararg params: Any): Timestamp? {
var retValue: Timestamp? = null
var pstmt: PreparedStatement? = null
var rs: ResultSet? = null
try {
pstmt = prepareStatement(sql, trxName)
setParameters(pstmt, params)
rs = pstmt!!.executeQuery()
if (rs!!.next())
retValue = rs!!.getTimestamp(1)
else if (log.isLoggable(Level.FINE)) log.fine("No Value $sql")
} catch (e: SQLException) {
throw DBException(e, sql)
} finally
{
close(rs, pstmt)
rs = null
pstmt = null
}
return retValue
}

/**
 * Get BigDecimal Value from sql
 *
 * @param trxName trx
 * @param sql sql
 * @param params collection of parameters
 * @return first value or null if not found
 * @throws DBException if there is any SQLException
 */
  @Throws(DBException::class)
 fun getSQLValueTSEx(trxName: String, sql: String, params: List<Any>): Timestamp? {
return getSQLValueTSEx(trxName, sql, *params.toTypedArray())
}

/**
 * Get Timestamp Value from sql
 *
 * @param trxName trx
 * @param sql sql
 * @param params array of parameters
 * @return first value or null
 */
   fun getSQLValueTS(trxName: String, sql: String, vararg params: Any): Timestamp? {
try {
return getSQLValueTSEx(trxName, sql, *params)
} catch (e: Exception) {
log.log(Level.SEVERE, sql, getSQLException(e))
}

return null
}

/**
 * Get Timestamp Value from sql
 *
 * @param trxName trx
 * @param sql sql
 * @param params collection of parameters
 * @return first value or null
 */
   fun getSQLValueTS(trxName: String, sql: String, params: List<Any>): Timestamp? {
val arr = arrayOfNulls<Any>(params.size)
params.toTypedArray()
return getSQLValueTS(trxName, sql, *arr)
}

/**
 * Get Array of Key Name Pairs
 *
 * @param sql select with id / name as first / second column
 * @param optional if true (-1,"") is added
 * @param params query parameters
 */
  @JvmOverloads fun getKeyNamePairs(sql: String, optional: Boolean, vararg params: Any = null as Array<Any>?): Array<KeyNamePair> {
return getKeyNamePairs(null, sql, optional, *params)
}

/**
 * Get Array of Key Name Pairs
 *
 * @param trxName
 * @param sql select with id / name as first / second column
 * @param optional if true (-1,"") is added
 * @param params query parameters
 */
   fun getKeyNamePairs(
       trxName: String?,
       sql: String,
       optional: Boolean,
       vararg params: Any
   ): Array<KeyNamePair> {
var pstmt: PreparedStatement? = null
var rs: ResultSet? = null
val list = ArrayList<KeyNamePair>()
if (optional) {
list.add(KeyNamePair(-1, ""))
}
try {
pstmt = DB.prepareStatement(sql, trxName)
setParameters(pstmt, params)
rs = pstmt!!.executeQuery()
while (rs!!.next()) {
list.add(KeyNamePair(rs!!.getInt(1), rs!!.getString(2)))
}
} catch (e: Exception) {
log.log(Level.SEVERE, sql, getSQLException(e))
} finally
{
close(rs, pstmt)
rs = null
pstmt = null
}
val retValue = arrayOfNulls<KeyNamePair>(list.size)
list.toTypedArray()
 //  s_log.fine("getKeyNamePairs #" + retValue.length);
    return retValue
} // 	getKeyNamePairs

/**
 * Get Array of IDs
 *
 * @param trxName
 * @param sql select with id as first column
 * @param params query parameters
 * @throws DBException if there is any SQLException
 */
  @Throws(DBException::class)
 fun getIDsEx(trxName: String, sql: String, vararg params: Any): IntArray {
var pstmt: PreparedStatement? = null
var rs: ResultSet? = null
val list = ArrayList<Int>()
try {
pstmt = DB.prepareStatement(sql, trxName)
setParameters(pstmt, params)
rs = pstmt!!.executeQuery()
while (rs!!.next()) {
list.add(rs!!.getInt(1))
}
} catch (e: SQLException) {
throw DBException(e, sql)
} finally
{
close(rs, pstmt)
rs = null
pstmt = null
}
 // 	Convert to array
    val retValue = IntArray(list.size)
for (i in retValue.indices) {
retValue[i] = list[i]
}
return retValue
} // 	getIDsEx

/**
 * ************************************************************************ Print SQL Warnings.
 * <br></br>
 * Usage: DB.printWarning("comment", rs.getWarnings());
 *
 * @param comment comment
 * @param warning warning
 */
   fun printWarning(comment: String?, warning: SQLWarning?) {
if (comment == null || warning == null || comment!!.length == 0) return
log.warning(comment)
 //
    var warn = warning
while (warn != null) {
val buffer = StringBuilder()
buffer
.append(warn!!.message)
.append("; State=")
.append(warn!!.sqlState)
.append("; ErrorCode=")
.append(warn!!.errorCode)
log.warning(buffer.toString())
warn = warn!!.nextWarning
}
} // 	printWarning

/**
 * Create SQL TO Date String from Timestamp
 *
 * @param time Date to be converted
 * @param dayOnly true if time set to 00:00:00
 * @return TO_DATE('2001-01-30 18:10:20',''YYYY-MM-DD HH24:MI:SS') or
 * TO_DATE('2001-01-30',''YYYY-MM-DD')
 */
   fun TO_DATE(time: Timestamp, dayOnly: Boolean): String {
return s_cc!!.getDatabase().TO_DATE(time, dayOnly)
} //  TO_DATE

/**
 * Create SQL TO Date String from Timestamp
 *
 * @param day day time
 * @return TO_DATE String (day only)
 */
   fun TO_DATE(day: Timestamp): String {
return TO_DATE(day, true)
} //  TO_DATE

/**
 * Create SQL for formatted Date, Number
 *
 * @param columnName the column name in the SQL
 * @param displayType Display Type
 * @param AD_Language 6 character language setting (from Env.LANG_*)
 * @return TRIM(TO_CHAR(columnName,'999G999G999G990D00','NLS_NUMERIC_CHARACTERS='',.''')) or
 * TRIM(TO_CHAR(columnName,'TM9')) depending on DisplayType and Language
 * @see org.idempiere.common.util.DisplayType
 *
 * @see org.idempiere.common.util.Env
 */
   fun TO_CHAR(columnName: String?, displayType: Int, AD_Language: String?): String {
if (columnName == null || AD_Language == null || columnName!!.length == 0)
throw IllegalArgumentException("Required parameter missing")
return s_cc!!.getDatabase().TO_CHAR(columnName, displayType, AD_Language)
} //  TO_CHAR

/**
 * Return number as string for INSERT statements with correct precision
 *
 * @param number number
 * @param displayType display Type
 * @return number as string
 */
   fun TO_NUMBER(number: BigDecimal, displayType: Int): String {
return s_cc!!.getDatabase().TO_NUMBER(number, displayType)
} // 	TO_NUMBER

/**
 * Package Strings for SQL command in quotes.
 *
 * <pre>
 * -	include in ' (single quotes)
 * -	replace ' with ''
</pre> *
 *
 * @param txt String with text
 * @param maxLength Maximum Length of content or 0 to ignore
 * @return escaped string for insert statement (NULL if null)
 */
  @JvmOverloads fun TO_STRING(txt: String?, maxLength: Int = 0): String {
if (txt == null || txt!!.length == 0) return "NULL"

 //  Length
    var text: String = txt
if (maxLength != 0 && text.length > maxLength) text = txt!!.substring(0, maxLength)

 //  copy characters		(we need to look through anyway)
    val out = StringBuilder()
out.append(QUOTE) // 	'
for (i in 0 until text.length) {
val c = text[i]
if (c == QUOTE)
out.append("''")
else
out.append(c)
}
out.append(QUOTE) // 	'
 //
    return out.toString()
} // 	TO_STRING

/**
 * convenient method to close result set
 *
 * @param rs
 */
   fun close(rs: ResultSet?) {
try {
if (rs != null) rs!!.close()
} catch (e: SQLException) {}
}

/**
 * convenient method to close statement
 *
 * @param st
 */
   fun close(st: Statement?) {
try {
if (st != null) st!!.close()
} catch (e: SQLException) {}
}

/**
 * Try to get the SQLException from Exception
 *
 * @param e Exception
 * @return SQLException if found or provided exception elsewhere
 */
   fun getSQLException(e: Exception?): Exception? {
var e1: Throwable? = e
while (e1 != null) {
if (e1 is SQLException) return e1 as SQLException?
e1 = e1!!.cause
}
return e
}

 fun getSQLValue(trxName: String, sql: String, int_param1: Int): Int {
return getSQLValue(trxName, sql, *arrayOf<Any>(int_param1))
}

 fun getSQLValue(trxName: String, sql: String, int_param1: Int, int_param2: Int): Int {
return getSQLValue(trxName, sql, *arrayOf<Any>(int_param1, int_param2))
}

 fun getSQLValue(trxName: String, sql: String, str_param1: String): Int {
return getSQLValue(trxName, sql, *arrayOf<Any>(str_param1))
}

 fun getSQLValue(trxName: String, sql: String, int_param1: Int, str_param2: String): Int {
return getSQLValue(trxName, sql, *arrayOf(int_param1, str_param2))
}

 fun getSQLValueString(trxName: String, sql: String, int_param1: Int): String? {
return getSQLValueString(trxName, sql, *arrayOf<Any>(int_param1))
}

 fun getSQLValueBD(trxName: String, sql: String, int_param1: Int): BigDecimal? {
return getSQLValueBD(trxName, sql, *arrayOf<Any>(int_param1))
}

/**
 * Get Array of ValueNamePair items.
 *
 * <pre> Example:
 * String sql = "SELECT Name, Description FROM AD_Ref_List WHERE AD_Reference_ID=?";
 * ValueNamePair[] list = DB.getValueNamePairs(sql, false, params);
</pre> *
 *
 * @param sql SELECT Value_Column, Name_Column FROM ...
 * @param optional if [ValueNamePair.EMPTY] is added
 * @param params query parameters
 * @return array of [ValueNamePair] or empty array
 * @throws DBException if there is any SQLException
 */
   fun getValueNamePairs(
       sql: String,
       optional: Boolean,
       params: List<Any>
   ): Array<ValueNamePair> {
var pstmt: PreparedStatement? = null
var rs: ResultSet? = null
val list = ArrayList<ValueNamePair>()
if (optional) {
list.add(ValueNamePair.EMPTY)
}
try {
pstmt = DB.prepareStatement(sql, null)
setParameters(pstmt, params)
rs = pstmt!!.executeQuery()
while (rs!!.next()) {
list.add(ValueNamePair(rs!!.getString(1), rs!!.getString(2)))
}
} catch (e: SQLException) {
throw DBException(e, sql)
} finally
{
close(rs, pstmt)
rs = null
pstmt = null
}
return list.toTypedArray()
}

/**
 * Get Array of KeyNamePair items.
 *
 * <pre> Example:
 * String sql = "SELECT C_City_ID, Name FROM C_City WHERE C_City_ID=?";
 * KeyNamePair[] list = DB.getKeyNamePairs(sql, false, params);
</pre> *
 *
 * @param sql SELECT ID_Column, Name_Column FROM ...
 * @param optional if [ValueNamePair.EMPTY] is added
 * @param params query parameters
 * @return array of [KeyNamePair] or empty array
 * @throws DBException if there is any SQLException
 */
   fun getKeyNamePairs(sql: String, optional: Boolean, params: List<Any>): Array<KeyNamePair> {
var pstmt: PreparedStatement? = null
var rs: ResultSet? = null
val list = ArrayList<KeyNamePair>()
if (optional) {
list.add(KeyNamePair.EMPTY)
}
try {
pstmt = DB.prepareStatement(sql, null)
setParameters(pstmt, params)
rs = pstmt!!.executeQuery()
while (rs!!.next()) {
list.add(KeyNamePair(rs!!.getInt(1), rs!!.getString(2)))
}
} catch (e: SQLException) {
throw DBException(e, sql)
} finally
{
close(rs, pstmt)
rs = null
pstmt = null
}
return list.toTypedArray()
}

/**
 * Create persistent selection in T_Selection table remain this function for backward
 * compatibility. refer: IDEMPIERE-1970
 *
 * @param AD_PInstance_ID
 * @param selection
 * @param trxName
 */
   fun createT_Selection(
       AD_PInstance_ID: Int,
       selection: Collection<Int>,
       trxName: String
   ) {
var insert = StringBuilder()
insert.append("INSERT INTO T_SELECTION(AD_PINSTANCE_ID, T_SELECTION_ID) ")
var counter = 0
for (selectedId in selection) {
counter++
if (counter > 1) insert.append(" UNION ")
insert.append("SELECT ")
insert.append(AD_PInstance_ID)
insert.append(", ")
insert.append(selectedId)
insert.append(" FROM DUAL ")

if (counter >= 1000) {
DB.executeUpdateEx(insert.toString(), trxName)
insert = StringBuilder()
insert.append("INSERT INTO T_SELECTION(AD_PINSTANCE_ID, T_SELECTION_ID) ")
counter = 0
}
}
if (counter > 0) {
DB.executeUpdateEx(insert.toString(), trxName)
}
}

/**
 * Create persistent selection in T_Selection table saveKeys is map with key is rowID, value is
 * list value of all viewID viewIDIndex is index of viewID need save.
 *
 * @param AD_PInstance_ID
 * @param selection
 * @param trxName
 */
   fun createT_SelectionNew(
       AD_PInstance_ID: Int,
       saveKeys: Collection<KeyNamePair>,
       trxName: String
   ) {
var insert = StringBuilder()
insert.append("INSERT INTO T_SELECTION(AD_PINSTANCE_ID, T_SELECTION_ID, ViewID) ")
var counter = 0
for (saveKey in saveKeys) {
val selectedId = saveKey.getKey()
counter++
if (counter > 1) insert.append(" UNION ")
insert.append("SELECT ")
insert.append(AD_PInstance_ID)
insert.append(", ")
insert.append(selectedId)
insert.append(", ")

val viewIDValue = saveKey.getName()
 // when no process have viewID or this process have no viewID or value of viewID is null
      if (viewIDValue == null) {
insert.append("NULL")
} else {
insert.append("'")
insert.append(viewIDValue)
insert.append("'")
}

insert.append(" FROM DUAL ")

if (counter >= 1000) {
DB.executeUpdateEx(insert.toString(), trxName)
insert = StringBuilder()
insert.append("INSERT INTO T_SELECTION(AD_PINSTANCE_ID, T_SELECTION_ID, ViewID) ")
counter = 0
}
}
if (counter > 0) {
DB.executeUpdateEx(insert.toString(), trxName)
}
}

private fun verifyTrx(trxName: String?, sql: String) {
if (trxName != null && Trx.get(trxName, false) == null) {
 // Using a trx that was previously closed or never opened
      // probably timed out - throw Exception (IDEMPIERE-644)
      val msg = "Transaction closed or never opened ($trxName) => (maybe timed out)"
log.error(msg) // severe
throw DBException(msg)
}
}

/**
 * @param tableName
 * @return true if table or view with name=tableName exists in db
 */
   fun isTableOrViewExists(tableName: String): Boolean {
val conn = connectionRO
var rs: ResultSet? = null
try {
val metadata = conn!!.metaData
val tblName: String
if (metadata.storesUpperCaseIdentifiers())
tblName = tableName.toUpperCase()
else if (metadata.storesLowerCaseIdentifiers())
tblName = tableName.toLowerCase()
else
tblName = tableName
rs = metadata.getTables(null, null, tblName, null)
if (rs!!.next()) {
return true
}
} catch (e: SQLException) {
e.printStackTrace()
} finally
{

try {
if (rs != null) rs!!.close()
conn!!.close()
} catch (e: SQLException) {}
}
return false
}

/**
 * Get an array of objects from sql (one per each column on the select clause), column indexing
 * starts with 0
 *
 * @param trxName trx
 * @param sql sql
 * @param params array of parameters
 * @return null if not found
 * @throws DBException if there is any SQLException
 */
   fun getSQLValueObjectsEx(trxName: String, sql: String, vararg params: Any): List<Any>? {
var retValue: MutableList<Any>? = ArrayList()
var pstmt: PreparedStatement? = null
var rs: ResultSet? = null
try {
pstmt = prepareStatement(sql, trxName)
setParameters(pstmt, params)
rs = pstmt!!.executeQuery()
val rsmd = rs!!.metaData
if (rs!!.next()) {
for (i in 1..rsmd.columnCount) {
val obj = rs!!.getObject(i)
if (rs!!.wasNull())
retValue!!.add(null)
else
retValue!!.add(obj)
}
} else {
retValue = null
}
} catch (e: SQLException) {
throw DBException(e, sql)
} finally
{
close(rs, pstmt)
rs = null
pstmt = null
}
return retValue
}

/**
 * Get an array of arrays of objects from sql (one per each row, and one per each column on the
 * select clause), column indexing starts with 0 WARNING: This method must be used just for
 * queries returning few records, using it for many records implies heavy memory consumption
 *
 * @param trxName trx
 * @param sql sql
 * @param params array of parameters
 * @return null if not found
 * @throws DBException if there is any SQLException
 */
   fun getSQLArrayObjectsEx(
       trxName: String,
       sql: String,
       vararg params: Any
   ): List<List<Any>>? {
val rowsArray = ArrayList<List<Any>>()
var pstmt: PreparedStatement? = null
var rs: ResultSet? = null
try {
pstmt = prepareStatement(sql, trxName)
setParameters(pstmt, params)
rs = pstmt!!.executeQuery()
val rsmd = rs!!.metaData
while (rs!!.next()) {
val retValue = ArrayList<Any>()
for (i in 1..rsmd.columnCount) {
val obj = rs!!.getObject(i)
if (rs!!.wasNull())
retValue.add(null)
else
retValue.add(obj)
}
rowsArray.add(retValue)
}
} catch (e: SQLException) {
throw DBException(e, sql)
} finally
{
close(rs, pstmt)
rs = null
pstmt = null
}
    return if (rowsArray.size == 0) null else rowsArray
}
} /**
 * Prepare Statement.
 *
 * @param sql sql statement
 * @param resultSetType - ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE,
 * ResultSet.TYPE_SCROLL_SENSITIVE
 * @param resultSetConcurrency - ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE
 * @return Prepared Statement r/o or r/w depending on concur
 */ // 	prepareStatement
/**
 * Create Read Only Statement
 *
 * @return Statement
 */ // 	createStatement
/**
 * Execute Update. saves "DBExecuteError" in Log
 *
 * @param sql sql
 * @return number of rows updated or -1 if error
 */ // 	executeUpdate
/**
 * Execute Update. saves "DBExecuteError" in Log
 *
 * @param sql sql
 * @param trxName optional transaction name
 * @return number of rows updated or -1 if error
 */ // 	executeUpdate
/**
 * Execute Update. saves "DBExecuteError" in Log
 *
 * @param sql sql
 * @param ignoreError if true, no execution error is reported
 * @param trxName transaction
 * @return number of rows updated or -1 if error
 */ // 	executeUpdate
/**
 * Execute Update. saves "DBExecuteError" in Log
 *
 * @param sql sql
 * @param param int param
 * @param trxName transaction
 * @return number of rows updated or -1 if error
 */ // 	executeUpdate
/**
 * Execute Update. saves "DBExecuteError" in Log
 *
 * @param sql sql
 * @param param int parameter
 * @param ignoreError if true, no execution error is reported
 * @param trxName transaction
 * @return number of rows updated or -1 if error
 */ // 	executeUpdate
/**
 * Execute Update. saves "DBExecuteError" in Log
 *
 * @param sql sql
 * @param params array of parameters
 * @param ignoreError if true, no execution error is reported
 * @param trxName optional transaction name
 * @return number of rows updated or -1 if error
 *//**
 * Execute Update and throw exception.
 *
 * @param sql
 * @param params statement parameters
 * @param trxName transaction
 * @return number of rows updated
 * @throws SQLException
 *//**
 * Execute Update and throw exception.
 *
 * @see {@link .executeUpdateEx
 */ // 	executeUpdateEx
/**
 * Get Array of Key Name Pairs
 *
 * @param sql select with id / name as first / second column
 * @param optional if true (-1,"") is added
 * @return array of [KeyNamePair]
 * @see .getKeyNamePairs
 *//**
 * Package Strings for SQL command in quotes
 *
 * @param txt String with text
 * @return escaped string for insert statement (NULL if null)
 */ //  TO_STRING
 // Following methods are kept for BeanShell compatibility.
 // See BF [ 2030233 ] Remove duplicate code from DB class
 // TODO: remove this when BeanShell will support varargs methods
 // 	DB
