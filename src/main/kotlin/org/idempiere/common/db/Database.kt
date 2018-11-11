package org.idempiere.common.db

import java.util.ArrayList
import org.idempiere.common.base.Service
import org.idempiere.common.util.CLogger
import org.idempiere.icommon.db.AdempiereDatabase

/**
 * General Database Constants and Utilities
 *
 * @author Jorg Janke
 * @version $Id: Database.java,v 1.3 2006/07/30 00:55:13 jjanke Exp $
 */
class Database {
    init {
        instance = this
    }

    fun setDatabase(databaseService: AdempiereDatabase) {
        Database.databaseService = databaseService
    }

    private fun doGetDatabaseNames(): Array<String> {
        val names = ArrayList<String>()
        val services = Service.Companion.locator().list(AdempiereDatabase::class.java).getServices()
        for (db in services) {
            names.add(db.getName())
        }
        return names.toTypedArray()
    }

    private fun doGetDatabaseFromURL(url: String?): AdempiereDatabase? {
        if (url == null) {
            log.severe("No Database URL")
            return null
        }
        if (url.indexOf("oracle") != -1) return getDatabase(DB_ORACLE)
        if (url.indexOf("postgresql") != -1) return getDatabase(DB_POSTGRESQL)

        log.severe("No Database for $url")
        return null
    }

    companion object {
        /** Logger  */
        private val log = CLogger.getCLogger(Database::class.java)

        /** Oracle ID  */
        var DB_ORACLE = "Oracle"
        /** PostgreSQL ID  */
        var DB_POSTGRESQL = "PostgreSQL"

        /** Connection Timeout in seconds  */
        var CONNECTION_TIMEOUT = 10

        /** Default Port  */
        val DB_ORACLE_DEFAULT_PORT = 1521
        /** Default Connection Manager Port  */
        val DB_ORACLE_DEFAULT_CM_PORT = 1630

        /** Default Port  */
        val DB_POSTGRESQL_DEFAULT_PORT = 5432

        protected var instance: Database? = null

        protected var databaseService: AdempiereDatabase? = null

        /**
         * Get Database by database Id.
         *
         * @return database
         */
        fun getDatabase(type: String): AdempiereDatabase? {
            return databaseService
        }

        val databaseNames: Array<String>
            get() = instance!!.doGetDatabaseNames()

        /**
         * Get Database Driver by url string. Access to database specific functionality.
         *
         * @param URL JDBC connection url
         * @return Adempiere Database Driver
         */
        fun getDatabaseFromURL(url: String): AdempiereDatabase? {
            return instance!!.doGetDatabaseFromURL(url)
        }
    }
} //  Database
