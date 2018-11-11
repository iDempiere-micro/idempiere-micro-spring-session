package org.idempiere.common.util

interface IEnvEventListener {

    fun onClearWindowContext(windowNo: Int)

    fun onReset(finalCall: Boolean)
}
