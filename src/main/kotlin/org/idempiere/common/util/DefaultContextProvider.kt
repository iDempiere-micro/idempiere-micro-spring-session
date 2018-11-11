package org.idempiere.common.util

import org.slf4j.LoggerFactory
import java.util.Properties

/** @author Low Heng Sin
 */
class DefaultContextProvider : ContextProvider {

    override val context: Properties
        get() = s_ctx

    override fun showURL(url: String) {
        try {
            val uri = java.net.URI(url)
            java.awt.Desktop.getDesktop().browse(uri)
        } catch (e: Exception) {
            log?.warn(e.localizedMessage)
        }
    }

    companion object {

        /** Logging  */
        private val log = LoggerFactory.getLogger(javaClass)

        private val s_ctx = Properties()
    }
}
