package org.idempiere.common.util

interface ServerContextURLHandler {

    fun showURL(url: String)

    companion object {
        val SERVER_CONTEXT_URL_HANDLER = "SERVER_CONTEXT_URL_HANDLER"
    }
}
