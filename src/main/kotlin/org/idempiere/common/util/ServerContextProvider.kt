package org.idempiere.common.util

import java.util.Properties

/** @author Low Heng Sin
 */
class ServerContextProvider private constructor() : ContextProvider {

    override val context: Properties get() = ServerContextProvider.context

    /** Show url at zk desktop  */
    override fun showURL(url: String) {
        val handler = context[ServerContextURLHandler.SERVER_CONTEXT_URL_HANDLER] as ServerContextURLHandler
        if (handler != null) handler!!.showURL(url)
    }

    companion object {

        private val context = ServerContextPropertiesWrapper()

        val INSTANCE = ServerContextProvider()
    }
}
