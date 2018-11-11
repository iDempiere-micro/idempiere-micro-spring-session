package org.idempiere.common.db

import java.sql.CallableStatement

/**
 * Interface to wrap CallableStatement
 *
 * @author Low Heng Sin
 */
interface CCallableStatement : CPreparedStatement, CallableStatement