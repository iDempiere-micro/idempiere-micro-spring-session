package org.idempiere.common.db

import java.sql.PreparedStatement

/**
 * Interface to wrap PreparedStatement
 *
 * @author Low Heng Sin
 */
interface CPreparedStatement : CStatement, PreparedStatement // 	CPreparedStatement