package org.idempiere.common.db

import org.idempiere.common.db.Database
import org.idempiere.common.db.SecurityPrincipal
import org.idempiere.common.util.CLogger
import org.idempiere.icommon.db.AdempiereDatabase
import org.springframework.stereotype.Component

import javax.sql.DataSource
import java.io.Serializable
import java.sql.Connection
import java.sql.SQLException
import java.util.logging.Level
import company.bigger.util.Ini
import org.idempiere.common.exceptions.AdempiereException
import org.idempiere.common.util.Ini

/**
 * Adempiere Connection Descriptor
 *
 * @author Jorg Janke
 * @author Marek Mosiewicz<marek.mosiewicz></marek.mosiewicz>@jotel.com.pl> - support for RMI over HTTP
 * @version $Id: CConnection.java,v 1.5 2006/07/30 00:55:13 jjanke Exp $
 */
@Component
class CConnection(val ini: Ini) : Serializable, Cloneable {

    /** Name of Connection   */
    /*************************************************************************
     * Get Name
     * @return connection name
     */
    /**
     * Set Name
     * @param name connection name
     */
    var name = "Standard" //  setName

    /** Application Host     */
    /*************
     * Get Application Host
     * @return apps host
     */
    /**
     * Set Application Host
     * @param apps_host apps host
     */
    var appsHost = "MyAppsServer"
        set(apps_host) {
            field = apps_host
            name = toString()
        }

    /** Database Type        */
    /**
     * Get Database Type
     * @return database type
     */
    /**
     * Set Database Type and default settings.
     * Checked against installed databases
     * @param type database Type, e.g. Database.DB_ORACLE
     */
    //  Oracle
    // begin vpj-cd e-evolution 09 ene 2006
    //  PostgreSQL
    // end vpj-cd e-evolution 09 ene 2006
    var type = ""
        set(type) {
            if (Database.getDatabase(type) != null) {
                field = type
                isDatabaseOK = false
            }
            if (isOracle) {
                if (dbPort != Database.DB_ORACLE_DEFAULT_PORT)
                    dbPort = Database.DB_ORACLE_DEFAULT_PORT
                fwPort = Database.DB_ORACLE_DEFAULT_CM_PORT
            } else {
                isBequeath = false
                isViaFirewall = false
                ssl = false
            }
            if (isPostgreSQL) {
                if (dbPort != Database.DB_POSTGRESQL_DEFAULT_PORT)
                    dbPort = Database.DB_POSTGRESQL_DEFAULT_PORT
            }
        } //  setType

    /** Database Host        */
    /*************
     * Get Database Host name
     * @return db host name
     */
    /**
     * Set Database host name
     * @param db_host db host
     */
    override var dbHost = "MyDBServer"
        set(db_host) {
            field = db_host
            name = toString()
            isDatabaseOK = false
        } // 	getDbHost
    // 	setDbHost
    /** Database Port        */
    /**
     * Get DB Port
     * @return port
     */
    /**
     * Set DB Port
     * @param db_port db port
     */
    override var dbPort = 0
        set(db_port) {
            field = db_port
            isDatabaseOK = false
        } // 	getDbPort
    // 	setDbPort
    /** Database name        */
    /**
     * Get Database Name (Service Name)
     * @return db name
     */
    /**
     * Set Database Name (Service Name)
     * @param db_name db name
     */
    override var dbName = "MyDBName"
        set(db_name) {
            field = db_name
            name = toString()
            isDatabaseOK = false
        } // 	getDbName
    // 	setDbName

    /** In Memory connection     */
    /**
     * Is it a bequeath connection
     * @return true if bequeath connection
     */
    /**
     * Set Bequeath
     * @param bequeath bequeath connection
     */
    var isBequeath = false
        set(bequeath) {
            field = bequeath
            isDatabaseOK = false
        }

    /** Connection uses Firewall     */
    /**
     * Is DB via Firewall
     * @return true if via firewall
     */
    /**
     * Method setViaFirewall
     * @param viaFirewall boolean
     */
    var isViaFirewall = false
        set(viaFirewall) {
            field = viaFirewall
            isDatabaseOK = false
        }
    /** Firewall host        */
    /**
     * Method getFwHost
     * @return String
     */
    /**
     * Method setFwHost
     * @param fw_host String
     */
    var fwHost = ""
        set(fw_host) {
            field = fw_host
            isDatabaseOK = false
        }
    /** Firewall port        */
    /**
     * Get Firewall port
     * @return firewall port
     */
    /**
     * Set Firewall port
     * @param fw_port firewall port
     */
    var fwPort = 0
        set(fw_port) {
            field = fw_port
            isDatabaseOK = false
        }

    /** Connection uses SSL     */
    /**
     * Method setSsl
     * @param viaSsl boolean
     */
    override var ssl = false
        set(viaSsl) {
            field = viaSsl
            isDatabaseOK = false
        }

    /** DB User name         */
    /**
     * Get Database User
     * @return db user
     */
    /**
     * Set Database User
     * @param db_uid db user id
     */
    override var dbUid = "idempiere"
        set(db_uid) {
            field = db_uid
            name = toString()
            isDatabaseOK = false
        } // 	getDbUid
    // 	setDbUid
    /** DB User password     */
    /**
     * Get Database Password
     * @return db password
     */
    /**
     * Set DB password
     * @param db_pwd db user password
     */
    override var dbPwd = "idempiere"
        set(db_pwd) {
            field = db_pwd
            isDatabaseOK = false
        } // 	getDbPwd
    // 	setDbPwd

    /** Database             */
    private var m_db: AdempiereDatabase? = null
    /** ConnectionException  */
    /**
     * Get Database Exception of last connection attempt
     * @return Exception or null
     */
    var databaseException: Exception? = null
        private set //  getConnectionException
    /**
     * Get Last Exception of Apps Server Connection attempt
     * @return Exception or null
     */
    @get:Synchronized
    val appsServerException: Exception? = null //  getAppsServerException

    /** Database Connection 	 */
    /**
     * Is Database Connection OK
     * @return true if database connection is OK
     */
    var isDatabaseOK = false
        private set //  isDatabaseOK

    /** Info                 */
    private var m_info = arrayOfNulls<String>(2)

    /**	Server Version		 */
    /**
     * Get Apps Server Version
     * @return db host name
     */
    val serverVersion: String? = null // 	getServerVersion

    /** DataSource      	 */
    private var m_ds: DataSource? = null
    /** DB Info				 */
    private var m_dbInfo: String? = null
    /**
     * @return web port
     */
    /**
     * set web port
     * @param webPort
     */
    var webPort: Int = 0
    /**
     * @return ssl port
     */
    /**
     * set ssl port
     * @param sslPort
     */
    var sslPort: Int = 0
    private val m_queryAppsServer: Boolean = false
    var appServerCredential: SecurityPrincipal? = null
        private set

    /**
     * Is Oracle DB
     * @return true if Oracle
     */
    val isOracle: Boolean
        get() = Database.DB_ORACLE == type //  isOracle

    /**
     * Is PostgreSQL DB
     * @return true if PostgreSQL
     */
    val isPostgreSQL: Boolean
        get() = Database.DB_POSTGRESQL == type //  isPostgreSQL

    /**
     * Has Server Connection
     * @return true if DataSource exists
     */
    val isDataSource: Boolean
        get() = m_ds != null // 	isDataSource

    /**
     * Get DB Version Info
     * @return info
     */
    val dbInfo: String
        get() {
            val dbInfo = m_dbInfo
            if (dbInfo != null)
                return dbInfo
            val sb = StringBuilder()
            var conn = getConnection(true,
                    Connection.TRANSACTION_READ_COMMITTED)
            if (conn != null) {
                try {
                    val dbmd = conn.metaData
                    sb.append(dbmd.databaseProductVersion)
                            .append(";").append(dbmd.driverVersion)
                    if (isDataSource)
                        sb.append(";DS")
                    conn.close()
                    m_dbInfo = sb.toString()
                } catch (e: Exception) {
                    log.log(Level.SEVERE, "", e)
                    sb.append(e.localizedMessage)
                }
            }
            return sb.toString()
        } //  toStringDetail

    /**
     * Get Info.
     * - Database, Driver, Status Info
     * @return info
     */
    val info: String
        get() {
            val sb = StringBuilder(if (m_info[0] != null) m_info[0] else "")
            sb.append(" - ").append(if (m_info[1] != null) m_info[1] else "")
                    .append("\n").append(database.toString())

            sb.append("\nDatabaseOK=").append(isDatabaseOK)

            return sb.toString()
        } //  getInfo

    /**
     * Get Database
     * @return database
     */
    //  different driver
    // 	test class loader ability
    val database: AdempiereDatabase
        get() {
            val db = m_db
            if (db == null || db.name != type) {
                m_db = Database.getDatabase(type)
                val db1 = m_db
                if (db1 != null) {
                    db1.getDataSource(this)
                    return db1
                }
            }
            return db ?: throw AdempiereException("Unable to get database")
        } //  getDatabase

    /**
     * Get Connection String
     * @return connection string
     */
    //  updates m_db
    val connectionURL: String
        get() {
            return database.getConnectionURL(this)
        } //  getConnectionURL

    /**
     * Get Status Info
     * @return info
     */
    val status: String
        get() {
            val sb = StringBuilder(appsHost)
            sb.append("{").append(dbHost)
                    .append("-").append(dbName)
                    .append("-").append(dbUid)
                    .append("}")
            val db = m_db
            if (db != null)
                sb.append(db.status)
            return sb.toString()
        } // 	getStatus

    init {
        setAttributes(ini.connection)
        set(this)
    } //  CConnection

    /**
     * Set Name
     */
    fun setName() {
        name = toString()
    } //  setName

    /**
     * Set Web Port
     * @param webPortString web port as String
     */
    fun setWebPort(webPortString: String?) {
        try {
            if (webPortString == null || webPortString.length == 0)
            else
                webPort = Integer.parseInt(webPortString)
        } catch (e: Exception) {
            log.severe(e.toString())
        }
    }

    /**
     * Set SSL Port
     * @param sslPortString web port as String
     */
    fun setSSLPort(sslPortString: String?) {
        try {
            if (sslPortString == null || sslPortString.length == 0)
            else
                sslPort = Integer.parseInt(sslPortString)
        } catch (e: Exception) {
            log.severe(e.toString())
        }
    }

    /**
     * Set DB Port
     * @param db_portString db port as String
     */
    fun setDbPort(db_portString: String?) {
        try {
            if (db_portString == null || db_portString.length == 0)
            else
                dbPort = Integer.parseInt(db_portString)
        } catch (e: Exception) {
            log.severe(e.toString())
        }
    } //  setDbPort

    /**
     * Method setSsl
     * @param viaSslString String
     */
    fun setSsl(viaSslString: String) {
        try {
            ssl = java.lang.Boolean.valueOf(viaSslString)
        } catch (e: Exception) {
            log.severe(e.toString())
        }
    }

    /**
     * Method setViaFirewall
     * @param viaFirewallString String
     */
    fun setViaFirewall(viaFirewallString: String) {
        try {
            isViaFirewall = java.lang.Boolean.valueOf(viaFirewallString)
        } catch (e: Exception) {
            log.severe(e.toString())
        }
    }

    /**
     * Set Firewall port
     * @param fw_portString firewall port as String
     */
    fun setFwPort(fw_portString: String?) {
        try {
            if (fw_portString == null || fw_portString.length == 0)
            else
                fwPort = Integer.parseInt(fw_portString)
        } catch (e: Exception) {
            log.severe(e.toString())
        }
    }

    /**
     * Set Bequeath
     * @param bequeathString bequeath connection as String (true/false)
     */
    fun setBequeath(bequeathString: String) {
        try {
            isBequeath = java.lang.Boolean.valueOf(bequeathString)
        } catch (e: Exception) {
            log.severe(e.toString())
        }
    } // 	setBequeath

    /**
     * Supports BLOB
     * @return true if BLOB is supported
     */
    fun supportsBLOB(): Boolean {
        return m_db!!.supportsBLOB()
    } //  supportsBLOB

    /**************************************************************************
     * Create DB Connection
     * @return data source != null
     */
    fun setDataSource(): Boolean {
        return m_ds != null
    } // 	setDataSource

    /**
     * Set Data Source
     * @param ds data source
     * @return data source != null
     */
    fun setDataSource(ds: DataSource?): Boolean {
        if (ds == null && m_ds != null)
            database.close()
        m_ds = ds
        return m_ds != null
    } // 	setDataSource

    /**
     * Get Server Connection
     * @return DataSource
     */
    fun getDataSource(): DataSource? {
        return m_ds
    } // 	getDataSource

    @Throws(SQLException::class)
    fun readInfo(conn: Connection) {
        val dbmd = conn.metaData
        m_info[0] = ("Database=" + dbmd.databaseProductName +
                " - " + dbmd.databaseProductVersion)
        m_info[0] = m_info[0]?.replace('\n', ' ')
        m_info[1] = ("Driver  =" + dbmd.driverName +
                " - " + dbmd.driverVersion)
        if (isDataSource)
            m_info[1] += " - via DataSource"
        m_info[1] = m_info[1]?.replace('\n', ' ')
        if (log.isLoggable(Level.CONFIG)) log.config(m_info[0] + " - " + m_info[1])
    }

    /*************************************************************************
     * Short String representation
     * @return appsHost{dbHost-dbName-uid}
     */
    override fun toString(): String {
        val sb = StringBuilder(appsHost)
        sb.append("{").append(dbHost)
                .append("-").append(dbName)
                .append("-").append(dbUid)
                .append("}")
        return sb.toString()
    } //  toString

    /**
     * Detail Info
     * @return info
     */
    fun toStringDetail(): String {
        val sb = StringBuilder(appsHost)
        sb.append("{").append(dbHost)
                .append("-").append(dbName)
                .append("-").append(dbUid)
                .append("}")
        //
        var conn = getConnection(true,
                Connection.TRANSACTION_READ_COMMITTED)
        if (conn != null) {
            try {
                val dbmd = conn.metaData
                sb.append("\nDatabase=" + dbmd.databaseProductName +
                        " - " + dbmd.databaseProductVersion)
                sb.append("\nDriver  =" + dbmd.driverName +
                        " - " + dbmd.driverVersion)
                if (isDataSource)
                    sb.append(" - via DS")
                conn.close()
            } catch (e: Exception) {
            }
        }
        return sb.toString()
    } //  toStringDetail

    /**
     * String representation.
     * Used also for Instanciation
     * @return string representation
     * @see .setAttributes
     */
    fun toStringLong(): String {
        val sb = StringBuilder("CConnection[")
        sb.append("name=").append(escape(name))
                .append(",AppsHost=").append(escape(appsHost))
                .append(",WebPort=").append(webPort)
                .append(",SSLPort=").append(sslPort)
                .append(",type=").append(escape(type))
                .append(",DBhost=").append(escape(dbHost))
                .append(",DBport=").append(dbPort)
                .append(",DBname=").append(escape(dbName))
                .append(",BQ=").append(isBequeath)
                .append(",FW=").append(isViaFirewall)
                .append(",SSL=").append(ssl)
                .append(",FWhost=").append(escape(fwHost))
                .append(",FWport=").append(fwPort)
                .append(",UID=").append(escape(dbUid))
                .append(",PWD=").append(escape(dbPwd))
                .append("]")
        return sb.toString()
    } // 	the format is read by setAttributes
    //  toStringLong

    private fun escape(value: String?): String? {
        val value1 = value ?: return null

        // use html like escape sequence to escape = and ,
        return value1
            .replace("=", "&eq;")
            .replace(",", "&comma;")
    }

    /**
     * Set Attributes from String (pares toStringLong())
     * @param attributes attributes
     */
    private fun setAttributes(attributes: String) {
        var attributes1 = attributes
        try {
            attributes1 = attributes1.substring(attributes1.indexOf("[") + 1)
            attributes1 = attributes1.substring(0, attributes1.indexOf("]"))
            val pairs = attributes1.split("[,]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (pair in pairs) {
                val pairComponents = pair.split("[=]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val key = pairComponents[0]
                val value = if (pairComponents.size == 2) unescape(pairComponents[1]) else ""
                if ("name".equals(key, ignoreCase = true)) {
                    name = value
                } else if ("AppsHost".equals(key, ignoreCase = true)) {
                    appsHost = value
                } else if ("type".equals(key, ignoreCase = true)) {
                    type = value
                } else if ("DBhost".equals(key, ignoreCase = true)) {
                    dbHost = value
                } else if ("DBport".equals(key, ignoreCase = true)) {
                    setDbPort(value)
                } else if ("DbName".equals(key, ignoreCase = true)) {
                    dbName = value
                } else if ("BQ".equals(key, ignoreCase = true)) {
                    setBequeath(value)
                } else if ("FW".equals(key, ignoreCase = true)) {
                    setViaFirewall(value)
                } else if ("SSL".equals(key, ignoreCase = true)) {
                    setSsl(value)
                } else if ("FWhost".equals(key, ignoreCase = true)) {
                    fwHost = value
                } else if ("FWport".equals(key, ignoreCase = true)) {
                    setFwPort(value)
                } else if ("UID".equals(key, ignoreCase = true)) {
                    dbUid = value
                } else if ("PWD".equals(key, ignoreCase = true)) {
                    dbPwd = value
                } else if ("WebPort".equals(key, ignoreCase = true)) {
                    setWebPort(value)
                } else if ("SSLPort".equals(key, ignoreCase = true)) {
                    setSSLPort(value)
                }
            }
        } catch (e: Exception) {
            log.log(Level.SEVERE, attributes1 + " - " + e.toString(), e)
        }
    } //  setAttributes

    private fun unescape(value: String): String {
        var value1 = value
        value1 = value1.replace("&eq;", "=")
        value1 = value1.replace("&comma;", ",")
        return value1
    }

    /**
     * Equals
     * @param other object
     * @return true if other equals this
     */
    override fun equals(other: Any?): Boolean {
        if (other is CConnection) {
            val cc = other as CConnection?
            if (cc!!.appsHost == appsHost &&
                    cc.dbHost == dbHost &&
                    cc.dbPort == dbPort &&
                    cc.dbName == dbName &&
                    cc.type == type &&
                    cc.dbUid == dbUid &&
                    cc.dbPwd == dbPwd)
                return true
        }
        return false
    } //  equals

    /*************************************************************************
     * Hashcode
     * @return hashcode of name
     */
    override fun hashCode(): Int {
        return name.hashCode()
    } //  hashCode

    /**
     * Create Connection - no not close.
     * Sets m_dbException
     * @param autoCommit true if autocommit connection
     * @param transactionIsolation Connection transaction level
     * @return Connection
     */
    fun getConnection(autoCommit: Boolean, transactionIsolation: Int): Connection? {
        var conn: Connection? = null
        databaseException = null
        isDatabaseOK = false
        //
        database //  updates m_db
        val db = m_db
        if (db == null) {
            databaseException = IllegalStateException("No Database Connector")
            return null
        }
        //

        try {
            // 	if (!Ini.getIni().isClient()			//	Server
            // 		&& trxLevel != Connection.TRANSACTION_READ_COMMITTED)		// PO_LOB.save()
            // 	{
            // Exception ee = null;
            try {
                conn = db.getCachedConnection(this, autoCommit, transactionIsolation)
            } catch (e: Exception) {
                // ee = e;
                println("E1:" + e.toString())
                e.printStackTrace()
            }

            // 	Verify Connection
            if (conn != null) {
                if (conn.transactionIsolation != transactionIsolation)
                    conn.transactionIsolation = transactionIsolation
                if (conn.autoCommit != autoCommit)
                    conn.autoCommit = autoCommit
                isDatabaseOK = true
            } else {
                println("Unable to obtain connection. We will try to restart.")
                db.fubar()
            }
        } catch (ule: UnsatisfiedLinkError) {
            println("E2:" + ule.toString())
            ule.printStackTrace()

            val msg = (ule.localizedMessage +
                    " -> Did you set the LD_LIBRARY_PATH ? - " + connectionURL)
            databaseException = Exception(msg)
            log.severe(msg)
        } catch (ex: SQLException) {
            println("E3:" + ex.toString())
            ex.printStackTrace()

            databaseException = ex
            if (conn == null) {
                // log might cause infinite loop since it will try to acquire database connection again
                /*
				log.log(Level.SEVERE, getConnectionURL ()
					+ ", (1) AutoCommit=" + autoCommit + ",TrxIso=" + getTransactionIsolationInfo(transactionIsolation)
					+ " - " + ex.getMessage());
				*/
                System.err.println(connectionURL +
                        ", (1) AutoCommit=" + autoCommit + ",TrxIso=" + getTransactionIsolationInfo(transactionIsolation) +
                        " - " + ex.message)
            } else {
                try {
                    log.severe(connectionURL +
                            ", (2) AutoCommit=" + conn.autoCommit + "->" + autoCommit +
                            ", TrxIso=" + getTransactionIsolationInfo(conn.transactionIsolation) + "->" + getTransactionIsolationInfo(transactionIsolation) +
                            // 	+ " (" + getDbUid() + "/" + getDbPwd() + ")"
                            " - " + ex.message)
                } catch (ee: Exception) {
                    log.severe(connectionURL +
                            ", (3) AutoCommit=" + autoCommit + ", TrxIso=" + getTransactionIsolationInfo(transactionIsolation) +
                            // 	+ " (" + getDbUid() + "/" + getDbPwd() + ")"
                            " - " + ex.message)
                }
            }
        } catch (ex: Exception) {
            println("E4:" + ex.toString())
            ex.printStackTrace()

            databaseException = ex
            // log might cause infinite loop since it will try to acquire database connection again
            // log.log(Level.SEVERE, getConnectionURL(), ex);
            System.err.println(connectionURL + " - " + ex.localizedMessage)
        }

        // 	System.err.println ("CConnection.getConnection - " + conn);
        return conn
    } //  getConnection

    /**
     * Convert Statement
     * @param origStatement original statement (Oracle notation)
     * @return converted Statement
     * @throws Exception
     */
    @Throws(Exception::class)
    fun convertStatement(origStatement: String): String {
        //  make sure we have a good database
        val db = m_db
        if (db != null && db.name != type)
            database
        if (db != null)
            return db.convertStatement(origStatement)
        throw Exception(
                "CConnection.convertStatement - No Converstion Database")
    } //  convertStatement

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        val c = super.clone() as CConnection
        val info = arrayOfNulls<String>(2)
        info[0] = m_info[0]
        info[1] = m_info[1]
        c.m_info = info
        return c
    }

    companion object {
        /** Connection       */
        @Volatile
        private var s_cc: CConnection? = null

        /**
         * Set default client/server Connection for non Spring usages
         */
        fun set(cc: CConnection) {
            s_cc = cc
        }

        /**
         * Get default client/server Connection
         * @return Connection Descriptor
         */
        fun get(): CConnection {
            return s_cc!!
        } // 	get

        /**
         *
         */
        private const val serialVersionUID = -858558852550858165L

        /** Logger			 */
        private val log = CLogger.getCLogger(CConnection::class.java)

        /**
         * Get Transaction Isolation Info
         * @param transactionIsolation trx iso
         * @return clear test
         */
        fun getTransactionIsolationInfo(transactionIsolation: Int): String {
            if (transactionIsolation == Connection.TRANSACTION_NONE)
                return "NONE"
            if (transactionIsolation == Connection.TRANSACTION_READ_COMMITTED)
                return "READ_COMMITTED"
            if (transactionIsolation == Connection.TRANSACTION_READ_UNCOMMITTED)
                return "READ_UNCOMMITTED"
            if (transactionIsolation == Connection.TRANSACTION_REPEATABLE_READ)
                return "REPEATABLE_READ"
            return if (transactionIsolation == Connection.TRANSACTION_READ_COMMITTED) "SERIALIZABLE" else "<?$transactionIsolation?>"
        } // 	getTransactionIsolationInfo
    }
} //  CConnection