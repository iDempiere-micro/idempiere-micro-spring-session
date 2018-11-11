package org.idempiere.common.util

import java.util.Properties

/** @author Low Heng Sin
 */
interface ContextProvider {

    val context: Properties

    fun showURL(url: String)
}
