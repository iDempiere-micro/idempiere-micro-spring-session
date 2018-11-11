package company.bigger.service

import company.bigger.dto.ILogin
import company.bigger.dto.UserLoginModelResponse
import org.compiere.crm.MUser
import software.hsharp.core.models.INameKeyPair
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Properties
import java.util.Date
import java.util.logging.Level
import org.compiere.model.I_M_Warehouse
import org.compiere.orm.MRole
import org.compiere.orm.MSysConfig
import org.compiere.orm.MSystem
import org.compiere.orm.MTree_Base
import org.compiere.orm.Query
import org.idempiere.common.util.CLogger
import org.idempiere.common.util.DB
import org.idempiere.common.util.Env
import org.idempiere.common.util.KeyNamePair
import org.idempiere.common.util.Util
import org.idempiere.common.util.Language
import org.springframework.stereotype.Service

@Service
class LoginService {
    companion object {
        private const val dateFormatOnlyForCtx = "yyyy-MM-dd"
        @JvmField val log = CLogger.getCLogger(LoginService::class.java)
        private const val C_BPARTNER_ID = "#C_BPartner_Id"
    }

    private fun getOrgsAddSummary(
        list: ArrayList<KeyNamePair>,
        Summary_Org_ID: Int,
        Summary_Name: String,
        role: MRole?
    ) {
        val m_ctx = Env.getCtx()
        if (role == null) {
            log.warning("Summary Org=$Summary_Name($Summary_Org_ID) - No Role")
            return
        }
        // 	Do we look for trees?
        if (role.aD_Tree_Org_ID == 0) {
            if (log.isLoggable(Level.CONFIG)) log.config("Summary Org=$Summary_Name($Summary_Org_ID) - No Org Tree: $role")
            return
        }
        // 	Summary Org - Get Dependents
        val tree = MTree_Base.get(m_ctx, role.aD_Tree_Org_ID, null)
        val sql = ("SELECT AD_Client_ID, AD_Org_ID, Name, IsSummary FROM AD_Org " +
                "WHERE IsActive='Y' AND AD_Org_ID IN (SELECT Node_ID FROM " +
                tree.nodeTableName +
                " WHERE AD_Tree_ID=? AND Parent_ID=? AND IsActive='Y') " +
                "ORDER BY Name")
        var pstmt: PreparedStatement? = null
        var rs: ResultSet? = null
        try {
            pstmt = DB.prepareStatement(sql, null)
            pstmt!!.setInt(1, tree.aD_Tree_ID)
            pstmt.setInt(2, Summary_Org_ID)
            rs = pstmt.executeQuery()
            while (rs.next()) {
                // int AD_Client_ID = rs.getInt(1);
                val AD_Org_ID = rs.getInt(2)
                val Name = rs.getString(3)
                val summary = "Y" == rs.getString(4)
                //
                if (summary)
                    getOrgsAddSummary(list, AD_Org_ID, Name, role)
                else {
                    val p = KeyNamePair(AD_Org_ID, Name)
                    if (!list.contains(p))
                        list.add(p)
                }
            }
        } catch (e: Exception) {
            log.log(Level.SEVERE, sql, e)
        } finally {
            DB.close(rs, pstmt)
        }
    } // 	getOrgAddSummary

    fun getOrgs(rol: INameKeyPair): Array<INameKeyPair> {
        val m_ctx = Env.getCtx()
        if (Env.getContext(m_ctx, "#AD_Client_ID").length == 0)
        // 	could be number 0
            throw UnsupportedOperationException("Missing Context #AD_Client_ID")

        val AD_Client_ID = Env.getContextAsInt(m_ctx, "#AD_Client_ID")
        val AD_User_ID = Env.getContextAsInt(m_ctx, "#AD_User_ID")
        // 	s_log.fine("Client: " + client.toStringX() + ", AD_Role_ID=" + AD_Role_ID);

        // 	get Client details for role
        val list = ArrayList<KeyNamePair>()
        //
        val sql = (" SELECT DISTINCT r.UserLevel, r.ConnectionProfile,o.AD_Org_ID,o.Name,o.IsSummary " +
                " FROM AD_Org o" +
                " INNER JOIN AD_Role r on (r.AD_Role_ID=?)" +
                " INNER JOIN AD_Client c on (c.AD_Client_ID=?)" +
                " WHERE o.IsActive='Y' " +
                " AND o.AD_Client_ID IN (0, c.AD_Client_ID)" +
                " AND (r.IsAccessAllOrgs='Y'" +
                " OR (r.IsUseUserOrgAccess='N' AND o.AD_Org_ID IN (SELECT AD_Org_ID FROM AD_Role_OrgAccess ra" +
                " WHERE ra.AD_Role_ID=r.AD_Role_ID AND ra.IsActive='Y')) " +
                " OR (r.IsUseUserOrgAccess='Y' AND o.AD_Org_ID IN (SELECT AD_Org_ID FROM AD_User_OrgAccess ua" +
                " WHERE ua.AD_User_ID=?" +
                " AND ua.IsActive='Y')))" +
                "ORDER BY o.Name")
        //
        var pstmt: PreparedStatement? = null
        var role: MRole? = null
        var rs: ResultSet? = null
        try {
            pstmt = DB.prepareStatement(sql, null)
            pstmt!!.setInt(1, rol.Key)
            pstmt.setInt(2, AD_Client_ID)
            pstmt.setInt(3, AD_User_ID)
            rs = pstmt.executeQuery()
            //  load Orgs
            if (!rs.next()) {
                log.log(Level.SEVERE, "No org for Role: " + rol.toString())
                return arrayOf()
            }
            //  Role Info
            Env.setContext(m_ctx, "#AD_Role_ID", rol.Key)
            Env.setContext(m_ctx, "#AD_Role_Name", rol.name)
            // 	User Level
            Env.setContext(m_ctx, "#User_Level", rs.getString(1)) // 	Format 'SCO'
            //  load Orgs

            do {
                val AD_Org_ID = rs.getInt(3)
                val Name = rs.getString(4)
                val summary = "Y" == rs.getString(5)
                if (summary) {
                    if (role == null)
                        role = MRole.get(m_ctx, rol.Key)
                    getOrgsAddSummary(list, AD_Org_ID, Name, role)
                } else {
                    val p = KeyNamePair(AD_Org_ID, Name)
                    if (!list.contains(p))
                        list.add(p)
                }
            } while (rs.next())

            if (log.isLoggable(Level.FINE)) log.fine("Client: " + AD_Client_ID + ", AD_Role_ID=" + rol.name + ", AD_User_ID=" + AD_User_ID + " - orgs #" + list.count())
        } catch (ex: SQLException) {
            log.log(Level.SEVERE, sql, ex)
        } finally {
            DB.close(rs, pstmt)
        }

        if (list.count() == 0) {
            log.log(Level.WARNING, "No Org for Client: " + AD_Client_ID +
                    ", AD_Role_ID=" + rol.Key +
                    ", AD_User_ID=" + AD_User_ID)
            return arrayOf()
        }
        return list.toTypedArray()
    }

    fun getClients(app_user: String, app_pwd: String): Array<INameKeyPair> {
        val m_ctx = Env.getCtx()
        if (log.isLoggable(Level.INFO)) log.info("User=$app_user")

        if (Util.isEmpty(app_user)) {
            log.warning("No Apps User")
            return arrayOf()
        }

        // 	Authentication
        var authenticated = false
        MSystem.get(m_ctx) ?: throw IllegalStateException("No System Info")

        if (app_pwd == "") {
            log.warning("No Apps Password")
            return arrayOf()
        }

        val hash_password = MSysConfig.getBooleanValue(MSysConfig.USER_PASSWORD_HASH, false)
        val clientList = ArrayList<KeyNamePair>()
        val clientsValidated = ArrayList<Int>()
        val users = findByUsername(app_user)

        if (users.isEmpty()) {
            log.saveError("UserPwdError", app_user, false)
            return arrayOf()
        }

        val MAX_PASSWORD_AGE = MSysConfig.getIntValue(MSysConfig.USER_LOCKING_MAX_PASSWORD_AGE_DAY, 0)
        val now = Date().time
        var validButLocked = false
        for (user in users) {
            if (clientsValidated.contains(user.getADClientID())) {
                log.severe("Two users with password with the same name/email combination on same tenant: $app_user")
                return arrayOf()
            }
            clientsValidated.add(user.getADClientID())
            val valid = when {
                authenticated -> true
                hash_password -> user.authenticateHash(app_pwd)
                else -> // password not hashed
                    user.password != null && user.password == app_pwd
            }
            // authenticated by ldap
            if (valid) {
                if (user.isLocked()) {
                    validButLocked = true
                    continue
                }

                if (authenticated) {
                    // use Ldap because don't check password age
                } else if (user.isExpired()) {} else if (MAX_PASSWORD_AGE > 0 && !user.isNoPasswordReset()) {
                    if (user.getDatePasswordChanged() == null)
                        user.setDatePasswordChanged(Timestamp(now))

                    val days = (now - user.getDatePasswordChanged().getTime()) / (1000 * 60 * 60 * 24)
                    if (days > MAX_PASSWORD_AGE) {
                        user.setIsExpired(true)
                    }
                }

                val sql = StringBuilder("SELECT  DISTINCT cli.AD_Client_ID, cli.Name, u.AD_User_ID, u.Name")
                sql.append(" FROM AD_User_Roles ur")
                        .append(" INNER JOIN AD_User u on (ur.AD_User_ID=u.AD_User_ID)")
                        .append(" INNER JOIN AD_Client cli on (ur.AD_Client_ID=cli.AD_Client_ID)")
                        .append(" WHERE ur.IsActive='Y'")
                        .append(" AND u.IsActive='Y'")
                        .append(" AND cli.IsActive='Y'")
                        .append(" AND ur.AD_User_ID=? ORDER BY cli.Name")
                var pstmt: PreparedStatement? = null
                var rs: ResultSet? = null
                try {
                    pstmt = DB.prepareStatement(sql.toString(), null)
                    pstmt!!.setInt(1, user.getAD_User_ID())
                    rs = pstmt.executeQuery()

                    while (rs.next()) {
                        val AD_Client_ID = rs.getInt(1)
                        val Name = rs.getString(2)
                        val p = KeyNamePair(AD_Client_ID, Name)
                        clientList.add(p)
                    }
                } catch (ex: SQLException) {
                    log.log(Level.SEVERE, sql.toString(), ex)
                } finally {
                    DB.close(rs, pstmt)
                }
            }
        }
        if (clientList.size > 0)
            authenticated = true

        if (authenticated) {
            if (log.isLoggable(Level.FINE)) log.fine("User=" + app_user + " - roles #" + clientList.count())

            for (user in users) {
                user.setFailedLoginCount(0)
                user.setDateLastLogin(Timestamp(now))
                if (!user.save())
                    log.severe("Failed to update user record with date last setSecurityContext (" + user + " / clientID = " + user.getADClientID() + ")")
            }
        } else if (validButLocked) {
            // User account ({0}) is locked, please contact the system administrator
        } else {
            for (user in users) {
                if (user.isLocked()) {
                    continue
                }

                val count = user.getFailedLoginCount() + 1

                var reachMaxAttempt = false
                val MAX_LOGIN_ATTEMPT = MSysConfig.getIntValue(MSysConfig.USER_LOCKING_MAX_LOGIN_ATTEMPT, 0)
                if (MAX_LOGIN_ATTEMPT in 1..count) {
                    // Reached the maximum number of setSecurityContext attempts, user account ({0}) is locked
                    reachMaxAttempt = true
                } else if (MAX_LOGIN_ATTEMPT > 0) {
                    if (count == MAX_LOGIN_ATTEMPT - 1) {
                        // Invalid User ID or Password (Login Attempts: {0} / {1})
                        reachMaxAttempt = false
                    } else {
                    }
                } else {
                    reachMaxAttempt = false
                }

                user.setFailedLoginCount(count)
                user.setIsLocked(reachMaxAttempt)
                user.setDateAccountLocked(if (user.isLocked()) Timestamp(now) else null)
                if (!user.save())
                    log.severe("Failed to update user record with increase failed setSecurityContext count")
            }
        }
        return clientList.toTypedArray()
    }

    /**
     * Check Login information and set context.
     * @returns true if setSecurityContext info are OK
     * @param AD_User_ID user
     * @param AD_Role_ID role
     * @param AD_Client_ID client
     * @param AD_Org_ID org
     */
    private fun checkUserAccess(AD_User_ID: Int, AD_Role_ID: Int, AD_Client_ID: Int, AD_Org: INameKeyPair?): KeyNamePair? {
        //  Get Login Info
        var loginInfo: String? = null
        var c_bpartner_id = -1
        //  Verify existence of User/Client/Org/Role and User's access to Client & Org
        val sql = """SELECT u.Name || '@' || c.Name || '.' || o.Name AS Text, u.c_bpartner_id, ? as ad_user_id
                FROM AD_User u, AD_Client c, AD_Org o, AD_User_Roles ur
                WHERE u.AD_User_ID=?
                 AND c.AD_Client_ID=?
                 AND (o.AD_Org_ID=? OR ?=0)
                 AND ur.AD_Role_ID=?
                 AND ur.AD_User_ID=u.AD_User_ID
                 AND (o.AD_Client_ID = 0 OR o.AD_Client_ID=c.AD_Client_ID)
                 AND c.AD_Client_ID IN (SELECT AD_Client_ID FROM AD_Role_OrgAccess ca WHERE ca.AD_Role_ID=ur.AD_Role_ID)
                 AND o.AD_Org_ID IN (SELECT AD_Org_ID FROM AD_Role_OrgAccess ca WHERE ca.AD_Role_ID=ur.AD_Role_ID) """
        var pstmt: PreparedStatement? = null
        var rs: ResultSet? = null
        try {
            pstmt = DB.prepareStatement(sql, null)
            pstmt!!.setInt(1, AD_User_ID)
            pstmt.setInt(2, AD_User_ID)
            pstmt.setInt(3, AD_Client_ID)
            val AD_Org_ID = if (AD_Org == null) { 0 } else { AD_Org.Key }
            pstmt.setInt(4, AD_Org_ID)
            pstmt.setInt(5, AD_Org_ID)
            pstmt.setInt(6, AD_Role_ID)
            rs = pstmt.executeQuery()
            if (rs!!.next()) {
                loginInfo = rs.getString(1)
                c_bpartner_id = rs.getInt(2)
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            DB.close(rs, pstmt)
        }

        //  not verified
        if (loginInfo == null)
            return null

        return KeyNamePair(c_bpartner_id, loginInfo)
    } //  checkUserAccess

    /**
     *
     * @param AD_User_ID
     * @param AD_Role_ID
     * @param AD_Client_ID
     * @param AD_Org_ID
     * @param M_Warehouse_ID
     * @param Lang
     * @return true if setSecurityContext is successful
     */
    fun setSecurityContext(
        ctx: Properties,
        AD_User_ID: Int,
        AD_Role: INameKeyPair,
        AD_Client: INameKeyPair,
        AD_Org: INameKeyPair?,
        M_Warehouse: INameKeyPair?,
        Lang: String?
    ): Boolean {
        val loginInfoFull = checkUserAccess(AD_User_ID, AD_Role.Key, AD_Client.Key, AD_Org)
                ?: return false
        val c_bpartner_id = loginInfoFull.key

        Env.setContext(ctx, "#AD_Language", Lang)
        val m_language = Language.getLanguage(Lang)
        Env.verifyLanguage(ctx, m_language)

        //  Set Date
        val ts = Timestamp(System.currentTimeMillis())

        val dateFormat4Timestamp = SimpleDateFormat(dateFormatOnlyForCtx)
        Env.setContext(ctx, "#Date", dateFormat4Timestamp.format(ts) + " 00:00:00") //  JDBC format
        if (log.isLoggable(Level.INFO)) log.info(" #Date = " + Env.getContextAsDate(ctx, "#Date"))

        if (M_Warehouse != null) { Env.setContext(ctx, "#M_Warehouse_ID", M_Warehouse.ID) }

        Env.setContext(ctx, Env.LANGUAGE, m_language.adLanguage)
        Env.setContext(ctx, C_BPARTNER_ID, c_bpartner_id)

        return true
    }

    fun getRoles(app_user: String, client: INameKeyPair): Array<INameKeyPair> {
        val m_ctx = Env.getCtx()

        val rolesList = ArrayList<KeyNamePair>()
        val sql = StringBuffer("SELECT u.AD_User_ID, r.AD_Role_ID,r.Name ")
                .append("FROM AD_User u")
                .append(" INNER JOIN AD_User_Roles ur ON (u.AD_User_ID=ur.AD_User_ID AND ur.IsActive='Y')")
                .append(" INNER JOIN AD_Role r ON (ur.AD_Role_ID=r.AD_Role_ID AND r.IsActive='Y') ")
        sql.append("WHERE u.Password IS NOT NULL AND ur.AD_Client_ID=? AND ")
        val email_login = MSysConfig.getBooleanValue(MSysConfig.USE_EMAIL_FOR_LOGIN, false)
        if (email_login)
            sql.append("u.EMail=?")
        else
            sql.append("COALESCE(u.LDAPUser,u.Name)=?")
        sql.append(" AND r.IsMasterRole='N'")
        sql.append(" AND u.IsActive='Y' AND EXISTS (SELECT * FROM AD_Client c WHERE u.AD_Client_ID=c.AD_Client_ID AND c.IsActive='Y')")
        // don't show roles without org access
        sql.append(" AND (")
        sql.append(" (r.isaccessallorgs='Y' OR EXISTS (SELECT 1 FROM AD_Role_OrgAccess ro WHERE ro.AD_Role_ID=r.AD_Role_ID AND ro.IsActive='Y'))")
        // show roll with isuseuserorgaccess = "Y" when Exist org in AD_User_Orgaccess
        sql.append(" OR ")
        sql.append(" (r.isuseuserorgaccess='Y' AND EXISTS (SELECT 1 FROM AD_User_Orgaccess uo WHERE uo.AD_User_ID=u.AD_User_ID AND uo.IsActive='Y')) ")
        sql.append(")")
        sql.append(" ORDER BY r.Name")

        var pstmt: PreparedStatement? = null
        var rs: ResultSet? = null
        // 	get Role details
        try {
            pstmt = DB.prepareStatement(sql.toString(), null)
            pstmt!!.setInt(1, client.Key)
            pstmt.setString(2, app_user)
            rs = pstmt.executeQuery()

            if (!rs.next()) {
                log.log(Level.SEVERE, "No Roles for Client: " + client.toString())
                return arrayOf()
            }

            //  load Roles
            do {
                val AD_Role_ID = rs.getInt(2)
                val Name = rs.getString(3)
                val p = KeyNamePair(AD_Role_ID, Name)
                rolesList.add(p)
            } while (rs.next())
            //
            if (log.isLoggable(Level.FINE)) log.fine("Role: " + client.toString() + " - clients #" + rolesList.count())
        } catch (ex: SQLException) {
            log.log(Level.SEVERE, sql.toString(), ex)
        } finally {
            DB.close(rs, pstmt)
        }
        // Client Info
        Env.setContext(m_ctx, "#AD_Client_ID", client.Key)
        Env.setContext(m_ctx, "#AD_Client_Name", client.name)
        return rolesList.toTypedArray()
    }

    fun getWarehouses(org: INameKeyPair): Array<INameKeyPair> {
        val list = ArrayList<KeyNamePair>()
        val sql = ("SELECT M_Warehouse_ID, Name FROM M_Warehouse " +
                "WHERE AD_Org_ID=? AND IsActive='Y' " +
                " AND " + I_M_Warehouse.COLUMNNAME_IsInTransit + "='N' " + // do not show in tranzit warehouses - teo_sarca [ 2867246 ]

                "ORDER BY Name")
        var pstmt: PreparedStatement? = null
        var rs: ResultSet? = null
        try {
            pstmt = DB.prepareStatement(sql, null)
            pstmt!!.setInt(1, org.Key)
            rs = pstmt.executeQuery()

            if (!rs.next()) {
                if (log.isLoggable(Level.INFO)) log.info("No Warehouses for Org: " + org.toString())
                return arrayOf()
            }

            //  load Warehousess
            do {
                val AD_Warehouse_ID = rs.getInt(1)
                val Name = rs.getString(2)
                val p = KeyNamePair(AD_Warehouse_ID, Name)
                list.add(p)
            } while (rs.next())

            if (log.isLoggable(Level.FINE))
                log.fine("Org: " + org.toString() +
                        " - warehouses #" + list.count())
        } catch (ex: SQLException) {
            log.log(Level.SEVERE, "getWarehouses", ex)
        } finally {
            DB.close(rs, pstmt)
        }
        return list.toTypedArray()
    }

    fun clearCurrentUser() {
        val ctx = Env.getCtx()
        ctx.setProperty(Env.AD_CLIENT_ID, "-1")
        Env.setContext(ctx, Env.AD_CLIENT_ID, "-1")
        Env.setContext(ctx, Env.AD_USER_ID, "-1")
        Env.setContext(ctx, "#AD_User_Name", "")
        Env.setContext(ctx, "#SalesRep_ID", "-1")
    }

    fun setCurrentUser(user: UserLoginModelResponse?) {
        if (user == null) clearCurrentUser()
        else {
            val ctx = Env.getCtx()
            val clientId = user.clientId.toString()
            val userId = user.userId.toString()
            ctx.setProperty(Env.AD_CLIENT_ID, clientId)
            Env.setContext(ctx, Env.AD_CLIENT_ID, clientId)
            Env.setContext(ctx, Env.AD_USER_ID, userId)
            Env.setContext(ctx, "#AD_User_Name", user.loginName)
            Env.setContext(ctx, "#SalesRep_ID", userId)
        }
    }

    fun login(login: ILogin): UserLoginModelResponse {
        val ctx = Env.getCtx()

        // HACK - this is needed before calling the list of clients, because the user will be logged in
        // HACK - and the information about the setSecurityContext success or failure need to be saved to the DB
        ctx.setProperty(Env.AD_CLIENT_ID, "" + login.clientId)
        Env.setContext(ctx, Env.AD_CLIENT_ID, "" + login.clientId)

        val clients = getClients(login.loginName, login.password)
        val client = clients.firstOrNull { clients.count() == 1 || it.Key == login.clientId }
        if (client != null) {
            ctx.setProperty(Env.AD_CLIENT_ID, client.ID)
            Env.setContext(ctx, Env.AD_CLIENT_ID, client.ID)

            val roles = getRoles(login.loginName, client)

            val user = MUser.get(ctx, login.loginName)
            if (user != null) {

                Env.setContext(ctx, Env.AD_USER_ID, user.ID)
                Env.setContext(ctx, "#AD_User_Name", user.name)
                Env.setContext(ctx, "#SalesRep_ID", user.ID)

                val role = roles.first { it.Key == login.roleId || roles.count() == 1 }
                // orgs
                val orgs = getOrgs(role)

                val org = orgs.firstOrNull { it.Key == login.orgId || orgs.count() == 1 }

                val warehouses = if (org == null) {
                    arrayOf()
                } else {
                    getWarehouses(org)
                }

                val warehouse = if (org == null) {
                    null
                } else {
                    warehouses.firstOrNull { it.Key == login.warehouseId || warehouses.count() == 1 }
                }

                val AD_User_ID = Env.getAD_User_ID(ctx)

                val logged =
                        setSecurityContext(
                                ctx,
                                AD_User_ID,
                                role,
                                client,
                                org,
                                warehouse,
                                login.language
                        )

                val result =
                        UserLoginModelResponse(logged, clients, roles, orgs, warehouses, null, login.loginName, client.Key, user.Key)

                return result
            }
        }
        return UserLoginModelResponse(loginName = login.loginName)
    }

    fun currentUser(): MUser? {
        val ctx = Env.getCtx()
        val userId = Env.getContext(ctx, Env.AD_USER_ID)
        if (userId.isNullOrEmpty()) return null
        return MUser.get(ctx, userId.toInt())
    }

    fun findByUsername(app_user: String?): List<MUser> {
        val m_ctx = Env.getCtx()
        if (log.isLoggable(Level.INFO)) log.info("User=$app_user")

        if (Util.isEmpty(app_user)) {
            log.warning("No Apps User")
            return listOf()
        }

        // 	Authentication
        MSystem.get(m_ctx) ?: throw IllegalStateException("No System Info")
        val email_login = MSysConfig.getBooleanValue(MSysConfig.USE_EMAIL_FOR_LOGIN, false)

        val where = StringBuilder("Password IS NOT NULL AND ")
        if (email_login)
            where.append("EMail=?")
        else
            where.append("COALESCE(LDAPUser,Name)=?")
        where.append(" AND")
                .append(" EXISTS (SELECT * FROM AD_User_Roles ur")
                .append("         INNER JOIN AD_Role r ON (ur.AD_Role_ID=r.AD_Role_ID)")
                .append("         WHERE ur.AD_User_ID=AD_User.AD_User_ID AND ur.IsActive='Y' AND r.IsActive='Y') AND ")
                .append(" EXISTS (SELECT * FROM AD_Client c")
                .append("         WHERE c.AD_Client_ID=AD_User.AD_Client_ID")
                .append("         AND c.IsActive='Y') AND ")
                .append(" AD_User.IsActive='Y'")

        val users: List<MUser> = Query(m_ctx, MUser.Table_Name, where.toString(), null)
                .setParameters(app_user)
                .setOrderBy(MUser.COLUMNNAME_AD_User_ID)
                .list()

        if (users.size == 0) {
            log.saveError("UserPwdError", app_user, false)
            return listOf()
        }

        val MAX_ACCOUNT_LOCK_MINUTES = MSysConfig.getIntValue(MSysConfig.USER_LOCKING_MAX_ACCOUNT_LOCK_MINUTES, 0)
        val MAX_INACTIVE_PERIOD_DAY = MSysConfig.getIntValue(MSysConfig.USER_LOCKING_MAX_INACTIVE_PERIOD_DAY, 0)
        val now = Date().time
        for (user in users) {
            if (MAX_ACCOUNT_LOCK_MINUTES > 0 && user.isLocked() && user.getDateAccountLocked() != null) {
                val minutes = (now - user.getDateAccountLocked().getTime()) / (1000 * 60)
                if (minutes > MAX_ACCOUNT_LOCK_MINUTES) {
                    var inactive = false
                    if (MAX_INACTIVE_PERIOD_DAY > 0 && user.getDateLastLogin() != null) {
                        val days = (now - user.getDateLastLogin().getTime()) / (1000 * 60 * 60 * 24)
                        if (days > MAX_INACTIVE_PERIOD_DAY)
                            inactive = true
                    }

                    if (!inactive) {
                        user.setIsLocked(false)
                        user.setDateAccountLocked(null)
                        user.setFailedLoginCount(0)
                        if (!user.save())
                            log.severe("Failed to unlock user account")
                    }
                }
            }

            if (MAX_INACTIVE_PERIOD_DAY > 0 && !user.isLocked() && user.getDateLastLogin() != null) {
                val days = (now - user.getDateLastLogin().getTime()) / (1000 * 60 * 60 * 24)
                if (days > MAX_INACTIVE_PERIOD_DAY) {
                    user.setIsLocked(true)
                    user.setDateAccountLocked(Timestamp(now))
                    if (!user.save())
                        log.severe("Failed to lock user account")
                }
            }
        }

        return users
    }
}